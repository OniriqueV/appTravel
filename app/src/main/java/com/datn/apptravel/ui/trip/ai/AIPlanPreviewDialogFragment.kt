package com.datn.apptravels.ui.trip.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravels.data.model.AISuggestedPlan
import com.datn.apptravels.databinding.DialogAiPlanPreviewBinding
import java.text.SimpleDateFormat
import java.util.*

class AIPlanPreviewDialogFragment : DialogFragment() {

    private var _binding: DialogAiPlanPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AIPlanPreviewAdapter
    private var scheduleDays = mutableListOf<PreviewScheduleDay>()
    private var onSaveListener: ((plans: List<AISuggestedPlan>) -> Unit)? = null

    // Trip constraints for validation
    private var tripStartDate: String = ""
    private var tripEndDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiPlanPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.75).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = AIPlanPreviewAdapter(
            scheduleDays,
            onPlanDeleted = {
                checkIfEmpty()
            },
            onPlanEdit = { plan, position ->
                showEditPlanDialog(plan, position)
            }
        )

        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSaveAll.setOnClickListener {
            val allPlans = scheduleDays.flatMap { it.plans }
            if (allPlans.isNotEmpty()) {
                onSaveListener?.invoke(allPlans)
                dismiss()
            }
        }
    }

    private fun showEditPlanDialog(plan: AISuggestedPlan, position: AIPlanPreviewAdapter.DayPlanPosition) {
        // Get all plans for overlap validation
        val allPlans = scheduleDays.flatMap { it.plans }

        val editDialog = EditPlanDialogFragment.newInstance(
            plan,
            tripStartDate,
            tripEndDate,
            allPlans
        )

        editDialog.setOnSaveListener { updatedPlan ->
            // Update the plan in the list
            adapter.updatePlan(position, updatedPlan)

            // Re-sort plans if date/time changed
            resortPlansIfNeeded()
        }

        editDialog.show(childFragmentManager, "EditPlanDialog")
    }

    private fun resortPlansIfNeeded() {
        // Re-group and sort all plans by date
        val allPlans = scheduleDays.flatMap { it.plans }
        scheduleDays.clear()
        scheduleDays.addAll(groupPlansByDate(allPlans))
        adapter.notifyDataSetChanged()
    }

    private fun checkIfEmpty() {
        scheduleDays.removeAll { it.plans.isEmpty() }

        if (scheduleDays.isEmpty()) {
            dismiss()
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    fun setPlans(planList: List<AISuggestedPlan>) {
        scheduleDays.clear()
        val groupedPlans = groupPlansByDate(planList)
        scheduleDays.addAll(groupedPlans)

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun groupPlansByDate(plans: List<AISuggestedPlan>): List<PreviewScheduleDay> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        // Group by date
        val grouped = plans.groupBy { plan ->
            try {
                val date = dateTimeFormat.parse(plan.startTime)
                if (date != null) dateFormat.format(date) else "unknown"
            } catch (e: Exception) {
                "unknown"
            }
        }

        // Sort by date and create PreviewScheduleDay objects
        return grouped.entries
            .sortedBy { it.key }
            .mapIndexed { index, entry ->
                // Sort plans within each day by start time
                val sortedPlans = entry.value.sortedBy { plan ->
                    try {
                        dateTimeFormat.parse(plan.startTime)?.time ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }

                PreviewScheduleDay(
                    date = entry.key,
                    dayNumber = index + 1,
                    plans = sortedPlans.toMutableList()
                )
            }
    }

    fun setTripConstraints(startDate: String, endDate: String) {
        tripStartDate = startDate
        tripEndDate = endDate
    }

    fun setOnSaveListener(listener: (plans: List<AISuggestedPlan>) -> Unit) {
        onSaveListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tripStartDate: String, tripEndDate: String): AIPlanPreviewDialogFragment {
            return AIPlanPreviewDialogFragment().apply {
                setTripConstraints(tripStartDate, tripEndDate)
            }
        }
    }
}