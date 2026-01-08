package com.datn.apptravels.ui.profile.statistics

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravels.data.repository.StatisticsRepository
import com.datn.apptravels.databinding.ActivityStatisticsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.NumberFormat
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val statisticsRepository: StatisticsRepository by inject()
    private val auth: FirebaseAuth by inject()

    private lateinit var planTypesAdapter: PlanTypesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadStatistics()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        planTypesAdapter = PlanTypesAdapter()
        binding.recyclerViewPlanTypes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPlanTypes.adapter = planTypesAdapter
    }

    private fun loadStatistics() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Calculate fresh statistics
            val result = statisticsRepository.calculateUserStatistics(userId)

            binding.progressBar.visibility = View.GONE

            result.onSuccess { statistics ->
                // Display total stats
                binding.tvTotalTrips.text = statistics.totalTrips.toString()
                binding.tvTotalPlans.text = statistics.totalPlans.toString()

                // Format expense
                val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                binding.tvTotalExpense.text = "${formatter.format(statistics.totalExpense)} VNĐ"

                // Display trip status (3 states)
                binding.tvUpcomingTrips.text = statistics.upcomingTrips.toString()
                binding.tvOngoingTrips.text = statistics.ongoingTrips.toString()
                binding.tvCompletedTrips.text = statistics.completedTrips.toString()

                // Display plans by type (ALL 13 types from PlanType enum)
                val planTypesList = mutableListOf<PlanTypeItem>()

                // Add all types in order matching PlanType enum
                val orderedTypes = listOf(
                    "NONE" to "Khác",
                    "LODGING" to "Chỗ ở",
                    "FLIGHT" to "Chuyến bay",
                    "RESTAURANT" to "Nhà hàng",
                    "TOUR" to "Tour du lịch",
                    "BOAT" to "Tàu thuyền",
                    "TRAIN" to "Tàu hỏa",
                    "RELIGION" to "Tôn giáo",
                    "CAR_RENTAL" to "Thuê xe",
                    "CAMPING" to "Cắm trại",
                    "THEATER" to "Rạp chiếu phim",
                    "SHOPPING" to "Mua sắm",
                    "ACTIVITY" to "Hoạt động"
                )

                orderedTypes.forEach { (type, vietnameseName) ->
                    val count = statistics.plansByType[type] ?: 0
                    val percentage = if (statistics.totalPlans > 0) {
                        (count * 100f / statistics.totalPlans)
                    } else 0f

                    planTypesList.add(
                        PlanTypeItem(
                            type = vietnameseName,
                            count = count,
                            percentage = percentage
                        )
                    )
                }

                android.util.Log.d("StatisticsActivity", "Plan types list size: ${planTypesList.size}")
                planTypesList.forEachIndexed { index, item ->
                    android.util.Log.d("StatisticsActivity", "[$index] ${item.type}: ${item.count} (${item.percentage}%)")
                }

                planTypesAdapter.submitList(planTypesList)

            }.onFailure {
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun getVietNameseTypeName(type: String): String {
        return when (type) {
            "NONE" -> "Khác"
            "LODGING" -> "Chỗ ở"
            "FLIGHT" -> "Chuyến bay"
            "RESTAURANT" -> "Nhà hàng"
            "TOUR" -> "Tour du lịch"
            "BOAT" -> "Tàu thuyền"
            "TRAIN" -> "Tàu hỏa"
            "RELIGION" -> "Tôn giáo"
            "CAR_RENTAL" -> "Thuê xe"
            "CAMPING" -> "Cắm trại"
            "THEATER" -> "Rạp chiếu phim"
            "SHOPPING" -> "Mua sắm"
            "ACTIVITY" -> "Hoạt động"
            else -> "Khác"
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

data class PlanTypeItem(
    val type: String,
    val count: Int,
    val percentage: Float
)