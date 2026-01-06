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
        // Set dialog to 75% screen width with wrap content height
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.75).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
            // Set transparent background to show rounded corners
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = AIPlanPreviewAdapter(scheduleDays) {
            // Called when any plan is deleted
            checkIfEmpty()
        }
        
        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSaveAll.setOnClickListener {
            // Flatten all plans from all days
            val allPlans = scheduleDays.flatMap { it.plans }
            if (allPlans.isNotEmpty()) {
                onSaveListener?.invoke(allPlans)
                dismiss()
            }
        }
    }

    private fun checkIfEmpty() {
        // Remove empty days
        scheduleDays.removeAll { it.plans.isEmpty() }
        
        // If no days left, dismiss dialog
        if (scheduleDays.isEmpty()) {
            dismiss()
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    fun setPlans(planList: List<AISuggestedPlan>) {
        // Group plans by date
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
                PreviewScheduleDay(
                    date = entry.key,
                    dayNumber = index + 1,
                    plans = entry.value.toMutableList()
                )
            }
    }

    fun setOnSaveListener(listener: (plans: List<AISuggestedPlan>) -> Unit) {
        onSaveListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AIPlanPreviewDialogFragment {
            return AIPlanPreviewDialogFragment()
        }
    }
}
