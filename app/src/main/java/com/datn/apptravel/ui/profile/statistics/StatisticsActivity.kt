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

                // Display trip status
                binding.tvCompletedTrips.text = statistics.completedTrips.toString()
                binding.tvUpcomingTrips.text = statistics.upcomingTrips.toString()

                // Display plans by type
                val planTypesList = statistics.plansByType.map { (type, count) ->
                    PlanTypeItem(
                        type = getVietNameseTypeName(type),
                        count = count,
                        percentage = if (statistics.totalPlans > 0) {
                            (count * 100f / statistics.totalPlans)
                        } else 0f
                    )
                }.sortedByDescending { it.count }

                planTypesAdapter.submitList(planTypesList)

            }.onFailure {
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun getVietNameseTypeName(type: String): String {
        return when (type) {
            "ACTIVITY" -> "Hoạt động"
            "LODGING" -> "Chỗ ở"
            "RESTAURANT" -> "Nhà hàng"
            "FLIGHT" -> "Máy bay"
            "BOAT" -> "Tàu thuyền"
            "CAR_RENTAL" -> "Thuê xe"
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