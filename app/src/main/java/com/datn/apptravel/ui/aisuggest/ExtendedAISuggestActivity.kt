package com.datn.apptravels.ui.aisuggest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravels.BuildConfig
import com.datn.apptravels.R
import com.datn.apptravels.databinding.ActivityExtendedAisuggestBinding
import com.datn.apptravels.ui.aisuggest.data.api.ApiClient
import com.datn.apptravels.ui.aisuggest.data.model.ApiResult
import com.datn.apptravels.ui.aisuggest.data.model.SavedItinerary
import com.datn.apptravels.ui.aisuggest.data.model.TravelRequest
import com.datn.apptravels.ui.aisuggest.data.repository.ExtendedTravelRepository
import com.datn.apptravels.ui.aisuggest.ui.viewmodel.ExtendedTravelViewModel
import com.datn.apptravels.ui.aisuggest.ui.viewmodel.ExtendedTravelViewModelFactory
import com.datn.apptravels.ui.aisuggest.ui.adapter.SavedItineraryAdapter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class ExtendedAISuggestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExtendedAisuggestBinding
    private var currentRequest: TravelRequest? = null
    private var currentItinerary: String = ""

    private var searchJob: kotlinx.coroutines.Job? = null
    private val searchCache = mutableMapOf<String, List<String>>()

    // Danh sách địa điểm phổ biến (Việt Nam + Quốc tế)
    private val popularPlaces = listOf(
        // Việt Nam
        "Hà Nội, Vietnam",
        "TP. Hồ Chí Minh, Vietnam",
        "Đà Nẵng, Vietnam",
        "Hội An, Vietnam",
        "Phú Quốc, Vietnam",
        "Nha Trang, Vietnam",
        "Đà Lạt, Vietnam",
        "Hạ Long, Vietnam",
        "Sapa, Vietnam",
        "Huế, Vietnam",
        // Châu Á
        "Bangkok, Thailand",
        "Singapore",
        "Tokyo, Japan",
        "Seoul, South Korea",
        "Bali, Indonesia",
        "Kuala Lumpur, Malaysia",
        "Hong Kong",
        "Dubai, UAE",
        // Châu Âu
        "Paris, France",
        "London, United Kingdom",
        "Rome, Italy",
        "Barcelona, Spain",
        // Châu Mỹ
        "New York, USA",
        "Los Angeles, USA",
        // Châu Đại Dương
        "Sydney, Australia",
        "Melbourne, Australia"
    )

    private val savedAdapter by lazy {
        SavedItineraryAdapter(
            onItemClick = { showItineraryDetail(it) },
            onDeleteClick = { confirmDelete(it) }
        )
    }

    private val viewModel: ExtendedTravelViewModel by viewModels {
        val apiService = ApiClient.create(BuildConfig.api)
        val repository = ExtendedTravelRepository(apiService, applicationContext)
        ExtendedTravelViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExtendedAisuggestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        if (BuildConfig.api.isBlank() || BuildConfig.GEOAPIFY_API_KEY.isBlank()) {
            showError("Lỗi: API_KEY hoặc GEOAPIFY_API_KEY chưa được cấu hình")
            binding.btnGenerate.isEnabled = false
            return
        }

        setupRecyclerView()
        setupDestinationSearch()
        setupPeopleSpinner()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvSavedList.apply {
            adapter = savedAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ExtendedAISuggestActivity)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "AI Travel Planner"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    // ========== 1. TÌM KIẾM ĐỊA ĐIỂM THẬT VỚI DEBOUNCE ==========
    private fun setupDestinationSearch() {
        // Sử dụng custom layout cho dropdown
        val adapter = ArrayAdapter<String>(
            this,
            R.layout.dropdown_item_place,
            mutableListOf()
        )

        binding.actvDestination.apply {
            setAdapter(adapter)
            threshold = 1 // Giảm xuống 1 để hiển thị gợi ý sớm hơn
            dropDownHeight = 800 // Tăng chiều cao dropdown

            // Hiển thị địa điểm phổ biến khi focus lần đầu
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if (adapter.count > 0) {
                        showDropDown()
                    } else if (text.isEmpty()) {
                        // Hiển thị địa điểm phổ biến
                        adapter.clear()
                        adapter.addAll(popularPlaces)
                        adapter.notifyDataSetChanged()
                        showDropDown()
                    }
                }
            }

            // Hiển thị dropdown khi click
            setOnClickListener {
                if (text.isEmpty()) {
                    adapter.clear()
                    adapter.addAll(popularPlaces)
                    adapter.notifyDataSetChanged()
                }
                if (adapter.count > 0) {
                    showDropDown()
                }
            }

            // Xử lý khi chọn item
            setOnItemClickListener { _, _, position, _ ->
                val selected = adapter.getItem(position)
                if (selected != null && selected != "Không tìm thấy địa điểm") {
                    setText(selected)
                    clearFocus()
                }
            }
        }

        // Tìm kiếm với debounce 400ms
        binding.actvDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Hủy job tìm kiếm trước đó
                searchJob?.cancel()

                if (query.isEmpty()) {
                    // Hiển thị địa điểm phổ biến
                    adapter.clear()
                    adapter.addAll(popularPlaces)
                    adapter.notifyDataSetChanged()
                    if (binding.actvDestination.hasFocus()) {
                        binding.actvDestination.showDropDown()
                    }
                    return
                }

                if (query.length < 2) {
                    // Lọc địa điểm phổ biến
                    val filtered = popularPlaces.filter {
                        it.contains(query, ignoreCase = true)
                    }
                    adapter.clear()
                    adapter.addAll(filtered)
                    adapter.notifyDataSetChanged()
                    if (filtered.isNotEmpty() && binding.actvDestination.hasFocus()) {
                        binding.actvDestination.showDropDown()
                    }
                    return
                }

                // Kiểm tra cache trước - hiển thị ngay lập tức
                if (searchCache.containsKey(query)) {
                    val cachedResults = searchCache[query] ?: emptyList()
                    adapter.clear()
                    adapter.addAll(cachedResults)
                    adapter.notifyDataSetChanged()

                    // Hiển thị dropdown nếu có kết quả
                    if (cachedResults.isNotEmpty()) {
                        binding.actvDestination.showDropDown()
                    }
                    return
                }

                // Debounce: đợi 400ms sau khi người dùng ngừng gõ
                searchJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(400)
                    viewModel.searchPlaces(query)
                }
            }
        })
    }

    private fun setupPeopleSpinner() {
        val peopleOptions = arrayOf("1 người", "2 người", "3 người", "4 người", "5+ người")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, peopleOptions)
        binding.actvPeople.setAdapter(adapter)
        binding.actvPeople.setText(peopleOptions[0], false)
    }

    private fun setupClickListeners() {
        // Tạo lịch trình
        binding.btnGenerate.setOnClickListener {
            if (!isNetworkAvailable()) {
                showError("Không có kết nối mạng")
                return@setOnClickListener
            }

            val destination = binding.actvDestination.text.toString()
            val daysText = binding.etDays.text.toString()
            val budgetText = binding.etBudget.text.toString()
            val peopleText = binding.actvPeople.text.toString()

            if (destination.isEmpty() || daysText.isEmpty() || budgetText.isEmpty()) {
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

            if (days !in 1..30 || budget <= 0) {
                showError("Số ngày (1-30) và ngân sách phải hợp lệ")
                return@setOnClickListener
            }

            val interests = getSelectedInterests()
            val request = TravelRequest(destination, days, budget, people, interests)
            currentRequest = request

            viewModel.generateItinerary(request)
        }

        // ========== 2. CHAT SỬA LỊCH TRÌNH ==========
        binding.btnSendChat.setOnClickListener {
            val message = binding.etChatMessage.text.toString()
            if (message.isBlank()) {
                showError("Vui lòng nhập tin nhắn")
                return@setOnClickListener
            }

            if (currentItinerary.isEmpty()) {
                showError("Chưa có lịch trình để chỉnh sửa")
                return@setOnClickListener
            }

            viewModel.chatToModify(message)
            binding.etChatMessage.setText("")
        }

        // ========== 3. LƯU LỊCH TRÌNH ==========
        binding.btnSave.setOnClickListener {
            if (currentItinerary.isEmpty()) {
                showError("Chưa có lịch trình để lưu")
                return@setOnClickListener
            }

            showSaveDialog()
        }

        // Xem danh sách đã lưu
        binding.btnViewSaved.setOnClickListener {
            viewModel.loadSavedItineraries()
            binding.cardSavedList.visibility = View.VISIBLE
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
        // Kết quả tìm kiếm địa điểm với cache
        viewModel.searchResults.observe(this) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val query = binding.actvDestination.text.toString().trim()

                    // Lưu vào cache
                    searchCache[query] = result.data

                    // Cập nhật adapter
                    val adapter = binding.actvDestination.adapter as ArrayAdapter<String>
                    adapter.clear()

                    if (result.data.isNotEmpty()) {
                        adapter.addAll(result.data)
                        adapter.notifyDataSetChanged()

                        // Tự động hiển thị dropdown nếu field đang focus
                        if (binding.actvDestination.hasFocus()) {
                            binding.actvDestination.showDropDown()
                        }
                    } else {
                        // Không có kết quả
                        adapter.add("Không tìm thấy địa điểm")
                        adapter.notifyDataSetChanged()
                        binding.actvDestination.showDropDown()
                    }
                }
                is ApiResult.Error -> {
                    // Không hiển thị lỗi để không làm phiền người dùng
                }
                is ApiResult.Loading -> {
                    // Có thể thêm loading indicator nếu muốn
                }
            }
        }

        // Lịch trình chính
        viewModel.itineraryResult.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    showLoading(true)
                    binding.cardResult.visibility = View.VISIBLE
                    binding.cardChat.visibility = View.GONE
                }

                is ApiResult.Success -> {
                    showLoading(false)
                    currentItinerary = result.data
                    binding.tvResult.text = result.data
                    binding.cardResult.visibility = View.VISIBLE
                    binding.cardChat.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                }

                is ApiResult.Error -> {
                    showLoading(false)
                    showError(result.message)
                    binding.cardResult.visibility = View.GONE
                }
            }
        }

        // Kết quả chat
        viewModel.chatResult.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.progressChat.visibility = View.VISIBLE
                    binding.btnSendChat.isEnabled = false
                }

                is ApiResult.Success -> {
                    binding.progressChat.visibility = View.GONE
                    binding.btnSendChat.isEnabled = true

                    // Cập nhật lịch trình
                    currentItinerary = result.data
                    binding.tvResult.text = result.data

                    Toast.makeText(this, "✅ Đã cập nhật lịch trình", Toast.LENGTH_SHORT).show()
                }

                is ApiResult.Error -> {
                    binding.progressChat.visibility = View.GONE
                    binding.btnSendChat.isEnabled = true
                    showError(result.message)
                }
            }
        }

        // Danh sách đã lưu
        viewModel.savedItineraries.observe(this) { list ->
            savedAdapter.submitList(list)
            if (list.isEmpty()) {
                binding.tvNoSaved.visibility = View.VISIBLE
                binding.rvSavedList.visibility = View.GONE
            } else {
                binding.tvNoSaved.visibility = View.GONE
                binding.rvSavedList.visibility = View.VISIBLE
            }
        }

        // Trạng thái lưu
        viewModel.saveStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "✅ Đã lưu lịch trình", Toast.LENGTH_SHORT).show()
            } else {
                showError("Không thể lưu lịch trình")
            }
        }
    }

    private fun showSaveDialog() {
        val request = currentRequest ?: return

        val editText = android.widget.EditText(this)
        editText.hint = "VD: Du lịch ${request.destination} ${request.days} ngày"
        editText.setText("Du lịch ${request.destination} ${request.days} ngày")
        editText.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(this)
            .setTitle("💾 Lưu Lịch Trình")
            .setMessage("Đặt tên cho lịch trình của bạn:")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val title = editText.text.toString().ifBlank {
                    "Lịch trình ${System.currentTimeMillis()}"
                }

                viewModel.saveItinerary(
                    title = title,
                    destination = request.destination,
                    days = request.days,
                    budget = request.budget,
                    people = request.people,
                    interests = request.interests,
                    content = currentItinerary
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvResult.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnGenerate.isEnabled = !isLoading
        binding.btnGenerate.text = if (isLoading) "⏳ Đang tạo..." else "🚀 Tạo Lịch Trình"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showItineraryDetail(itinerary: SavedItinerary) {
        AlertDialog.Builder(this)
            .setTitle(itinerary.title)
            .setMessage(itinerary.content)
            .setPositiveButton("Đóng", null)
            .setNeutralButton("Xóa") { _, _ ->
                confirmDelete(itinerary)
            }
            .show()
    }

    private fun confirmDelete(itinerary: SavedItinerary) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa lịch trình \"${itinerary.title}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteItinerary(itinerary.id)
                Toast.makeText(this, "✅ Đã xóa", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}