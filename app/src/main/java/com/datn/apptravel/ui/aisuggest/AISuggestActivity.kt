package com.datn.apptravel.ui.aisuggest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.datn.apptravel.BuildConfig
import com.datn.apptravel.ui.aisuggest.data.api.ApiClient
import com.datn.apptravel.ui.aisuggest.data.model.ApiResult
import com.datn.apptravel.ui.aisuggest.data.model.TravelRequest
import com.datn.apptravel.ui.aisuggest.data.repository.TravelRepository
import com.datn.apptravel.databinding.ActivityAisuggestBinding
import com.datn.apptravel.ui.aisuggest.ui.viewmodel.TravelViewModel
import com.datn.apptravel.ui.aisuggest.ui.viewmodel.TravelViewModelFactory
import com.google.android.material.chip.Chip

class AISuggestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAisuggestBinding

    private val viewModel: TravelViewModel by viewModels {
        val apiService = ApiClient.create(BuildConfig.api)
        val repository = TravelRepository(apiService)
        TravelViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAisuggestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        // Kiểm tra API key
        if (BuildConfig.GEOAPIFY_API_KEY.isBlank()) {
            showError("Lỗi: API_KEY chưa được cấu hình trong local.properties")
            binding.btnGenerate.isEnabled = false
            return
        }

        setupDestinationSpinner()
        setupPeopleSpinner()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        // Thiết lập toolbar với nút back
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "AI Travel Planner"
        }

        // Xử lý sự kiện click nút back
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupDestinationSpinner() {
        val destinations = arrayOf(
            "Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hội An",
            "Phú Quốc", "Nha Trang", "Đà Lạt", "Hạ Long",
            "Sapa", "Huế", "Vũng Tàu", "Quy Nhơn",
            "Cần Thơ", "Phan Thiết"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            destinations
        )

        binding.actvDestination.setAdapter(adapter)
        binding.actvDestination.setText(destinations[0], false)

        binding.actvDestination.setOnClickListener {
            binding.actvDestination.showDropDown()
        }
    }

    private fun setupPeopleSpinner() {
        val peopleOptions = arrayOf("1 người", "2 người", "3 người", "4 người", "5+ người")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            peopleOptions
        )

        binding.actvPeople.setAdapter(adapter)
        binding.actvPeople.setText(peopleOptions[0], false)

        binding.actvPeople.setOnClickListener {
            binding.actvPeople.showDropDown()
        }
    }

    private fun setupClickListeners() {
        binding.btnGenerate.setOnClickListener {

            if (!isNetworkAvailable()) {
                showError("Không có kết nối mạng. Vui lòng kiểm tra Internet.")
                return@setOnClickListener
            }

            val destination = binding.actvDestination.text.toString()
            val daysText = binding.etDays.text.toString()
            val budgetText = binding.etBudget.text.toString()
            val peopleText = binding.actvPeople.text.toString()

            if (destination.isEmpty() || daysText.isEmpty() ||
                budgetText.isEmpty() || peopleText.isEmpty()
            ) {
                showError("Vui lòng điền đầy đủ thông tin")
                return@setOnClickListener
            }

            val days = daysText.toIntOrNull() ?: 0
            val budget = budgetText.toLongOrNull() ?: 0L

            val people = when {
                peopleText.contains("1") -> 1
                peopleText.contains("2") -> 2
                peopleText.contains("3") -> 3
                peopleText.contains("4") -> 4
                else -> 5
            }

            if (days !in 1..30) {
                showError("Số ngày phải từ 1–30")
                return@setOnClickListener
            }

            if (budget <= 0) {
                showError("Ngân sách phải lớn hơn 0")
                return@setOnClickListener
            }

            val interests = getSelectedInterests()
            val request = TravelRequest(destination, days, budget, people, interests)

            viewModel.generateItinerary(request)
        }
    }

    private fun getSelectedInterests(): List<String> {
        val interests = mutableListOf<String>()
        for (i in 0 until binding.chipGroupInterests.childCount) {
            val chip = binding.chipGroupInterests.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                interests.add(chip.text.toString())
            }
        }
        return interests
    }

    private fun observeViewModel() {
        viewModel.itineraryResult.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    showLoading(true)
                    binding.cardResult.visibility = View.VISIBLE
                }

                is ApiResult.Success -> {
                    showLoading(false)
                    binding.tvResult.text = result.data
                    binding.cardResult.visibility = View.VISIBLE
                }

                is ApiResult.Error -> {
                    showLoading(false)
                    showError(result.message)
                    binding.cardResult.visibility = View.GONE
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        binding.tvResult.visibility =
            if (isLoading) View.GONE else View.VISIBLE

        binding.btnGenerate.isEnabled = !isLoading
        binding.btnGenerate.text =
            if (isLoading) "⏳ Đang tạo..." else "🚀 Tạo Lịch Trình"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // Xử lý nút back của hệ thống
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}