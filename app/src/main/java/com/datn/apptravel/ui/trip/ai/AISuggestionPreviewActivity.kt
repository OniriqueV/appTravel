package com.datn.apptravel.ui.trip.ai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravel.R
import com.datn.apptravel.data.model.AISuggestedPlan
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.databinding.ActivityAiSuggestionPreviewBinding
import com.datn.apptravel.ui.trip.ai.adapter.AISuggestionAdapter
import com.datn.apptravel.ui.trip.detail.plandetail.ActivityDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.BoatDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.CarRentalDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.FlightDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.LodgingDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.AISuggestionViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Màn hình hiển thị danh sách gợi ý từ AI
 * User có thể chọn các plan và xem chi tiết
 */
class AISuggestionPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiSuggestionPreviewBinding
    private val viewModel: AISuggestionViewModel by viewModel()

    private lateinit var adapter: AISuggestionAdapter
    private var tripId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiSuggestionPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getStringExtra("tripId")

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // RecyclerView
        adapter = AISuggestionAdapter(
            suggestions = emptyList(),
            onItemClick = { plan ->
                openPlanDetail(plan)
            },
            onSelectClick = { plan ->
                viewModel.toggleSelection(plan)
            }
        )

        binding.recyclerViewSuggestions.apply {
            layoutManager = LinearLayoutManager(this@AISuggestionPreviewActivity)
            adapter = this@AISuggestionPreviewActivity.adapter
        }

        // Action chips
        binding.chipSaveAll.setOnClickListener {
            viewModel.selectAll()
        }

        binding.chipDiscardAll.setOnClickListener {
            viewModel.deselectAll()
        }

        // Bottom action bar
        binding.btnSaveSelected.setOnClickListener {
            val selected = viewModel.getSelectedSuggestions()
            if (selected.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 plan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Redirect to detail screens for each selected plan
            Toast.makeText(
                this,
                "Đã chọn ${selected.size} plans. Bạn sẽ được chuyển đến màn hình chỉnh sửa.",
                Toast.LENGTH_LONG
            ).show()

            // Open first selected plan
            if (selected.isNotEmpty()) {
                openPlanDetail(selected.first())
            }
        }

        // Retry button
        binding.btnRetry.setOnClickListener {
            // Reload suggestions
            finish()
        }
    }

    private fun setupObservers() {
        // Suggestions
        viewModel.suggestions.observe(this) { suggestions ->
            if (suggestions.isNotEmpty()) {
                adapter.updateSuggestions(suggestions)
                binding.tvSuggestionsCount.text = "${suggestions.size} gợi ý"

                binding.loadingState.visibility = View.GONE
                binding.contentState.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
            } else {
                binding.loadingState.visibility = View.GONE
                binding.contentState.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }

            updateBottomBar(suggestions)
        }

        // Loading
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.loadingState.visibility = View.VISIBLE
                binding.contentState.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
            }
        }

        // Error
        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                binding.loadingState.visibility = View.GONE
                binding.contentState.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun updateBottomBar(suggestions: List<AISuggestedPlan>) {
        val selectedCount = suggestions.count { it.isSelected }

        if (selectedCount > 0) {
            binding.bottomActionBar.visibility = View.VISIBLE
            binding.tvSelectedCount.text = "$selectedCount plans đã chọn"
        } else {
            binding.bottomActionBar.visibility = View.GONE
        }
    }

    /**
     * Redirect đến Activity tương ứng với plan type
     * REUSE Intent cũ - KHÔNG TẠO FLOW MỚI
     */
    private fun openPlanDetail(plan: AISuggestedPlan) {

        val intent = when (plan.type) {
            PlanType.RESTAURANT ->
                Intent(this, RestaurantDetailActivity::class.java)

            PlanType.LODGING ->
                Intent(this, LodgingDetailActivity::class.java)

            PlanType.FLIGHT ->
                Intent(this, FlightDetailActivity::class.java)

            PlanType.BOAT ->
                Intent(this, BoatDetailActivity::class.java)

            PlanType.CAR_RENTAL ->
                Intent(this, CarRentalDetailActivity::class.java)

            PlanType.TRAIN ->
                Intent(this, CarRentalDetailActivity::class.java)

            PlanType.ACTIVITY,
            PlanType.TOUR,
            PlanType.THEATER,
            PlanType.SHOPPING,
            PlanType.CAMPING,
            PlanType.RELIGION ->
                Intent(this, ActivityDetailActivity::class.java)

            else -> {
                Toast.makeText(this, "Loại plan này chưa được hỗ trợ", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Pass data qua Intent - REUSE existing fields
        intent.putExtra("tripId", tripId)
        intent.putExtra("placeName", plan.title)
        intent.putExtra("placeAddress", plan.address)
        intent.putExtra("placeLatitude", plan.lat)
        intent.putExtra("placeLongitude", plan.lng)
        intent.putExtra("startTime", plan.startTime)
        intent.putExtra("photoUrl", plan.photoUrl)
        intent.putExtra("expense", plan.expense)
        intent.putExtra("description", plan.description)
        intent.putExtra("notes", plan.notes)
        intent.putExtra("fromAI", true) // Flag để biết là từ AI suggestion

        startActivity(intent)
    }

    companion object {
        const val EXTRA_TRIP_ID = "tripId"
        const val EXTRA_START_DATE = "startDate"
        const val EXTRA_END_DATE = "endDate"
    }
}