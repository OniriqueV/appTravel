package com.datn.apptravels.ui.trip.ai

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.datn.apptravels.R
import com.datn.apptravels.data.model.AISuggestedPlan
import com.datn.apptravels.data.model.PlanType
import com.datn.apptravels.databinding.DialogEditPlanBinding
import java.text.SimpleDateFormat
import java.util.*

class EditPlanDialogFragment : DialogFragment() {

    private var _binding: DialogEditPlanBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentPlan: AISuggestedPlan
    private var onSaveListener: ((plan: AISuggestedPlan) -> Unit)? = null

    // Date and time tracking
    private var selectedDate: Calendar = Calendar.getInstance()
    private var startTime: Calendar = Calendar.getInstance()

    // Trip constraints
    private var tripStartDate: Calendar? = null
    private var tripEndDate: Calendar? = null
    private var existingPlans: List<AISuggestedPlan> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPlanTypeDropdown()
        loadPlanData()
        setupListeners()
    }

    private fun setupPlanTypeDropdown() {
        val planTypes = listOf(
            "🎡 Hoạt động" to PlanType.ACTIVITY,
            "🍽️ Nhà hàng" to PlanType.RESTAURANT,
            "🏨 Lưu trú" to PlanType.LODGING,
            "✈️ Chuyến bay" to PlanType.FLIGHT,
            "🚄 Tàu hỏa" to PlanType.TRAIN,
            "⛴️ Thuyền" to PlanType.BOAT,
            "🚗 Thuê xe" to PlanType.CAR_RENTAL,
            "🗺️ Tour" to PlanType.TOUR,
            "🎭 Sân khấu" to PlanType.THEATER,
            "🛍️ Mua sắm" to PlanType.SHOPPING,
            "⛺ Cắm trại" to PlanType.CAMPING,
            "🙏 Tôn giáo" to PlanType.RELIGION
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            planTypes.map { it.first }
        )
        binding.actvPlanType.setAdapter(adapter)

        // Store mapping for later retrieval
        binding.actvPlanType.tag = planTypes
    }

    private fun loadPlanData() {
        binding.apply {
            etPlanName.setText(currentPlan.title)

            // Set plan type
            val planTypeText = when (currentPlan.type) {
                PlanType.ACTIVITY -> "🎡 Hoạt động"
                PlanType.RESTAURANT -> "🍽️ Nhà hàng"
                PlanType.LODGING -> "🏨 Lưu trú"
                PlanType.FLIGHT -> "✈️ Chuyến bay"
                PlanType.TRAIN -> "🚄 Tàu hỏa"
                PlanType.BOAT -> "⛴️ Thuyền"
                PlanType.CAR_RENTAL -> "🚗 Thuê xe"
                PlanType.TOUR -> "🗺️ Tour"
                PlanType.THEATER -> "🎭 Sân khấu"
                PlanType.SHOPPING -> "🛍️ Mua sắm"
                PlanType.CAMPING -> "⛺ Cắm trại"
                PlanType.RELIGION -> "🙏 Tôn giáo"
                else -> currentPlan.type.toString()
            }
            actvPlanType.setText(planTypeText, false)

            // Parse and set dates/times
            try {
                val startDate = apiDateTimeFormat.parse(currentPlan.startTime)
                if (startDate != null) {
                    startTime.time = startDate
                    selectedDate.time = startDate
                    tvSelectedDate.text = dateFormat.format(startDate)
                    tvStartTime.text = timeFormat.format(startDate)
                    tvStartTime.setTextColor(resources.getColor(R.color.black, null))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Set cost
            currentPlan.expense?.takeIf { it > 0 }?.let {
                etCost.setText(it.toString())
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnSelectDate.setOnClickListener {
                showDatePicker()
            }

            btnSelectStartTime.setOnClickListener {
                showTimePicker()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }

            btnSave.setOnClickListener {
                if (validateAndSave()) {
                    dismiss()
                }
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                binding.tvSelectedDate.text = dateFormat.format(selectedDate.time)
                binding.tvSelectedDate.setTextColor(resources.getColor(R.color.black, null))

                // Update start time calendar with new date
                startTime.set(Calendar.YEAR, year)
                startTime.set(Calendar.MONTH, month)
                startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set constraints based on trip dates
            if (tripStartDate != null) {
                datePicker.minDate = tripStartDate!!.timeInMillis
            }
            if (tripEndDate != null) {
                datePicker.maxDate = tripEndDate!!.timeInMillis
            }
        }.show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                startTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                startTime.set(Calendar.MINUTE, minute)
                startTime.set(Calendar.SECOND, 0)

                binding.tvStartTime.text = timeFormat.format(startTime.time)
                binding.tvStartTime.setTextColor(resources.getColor(R.color.black, null))
            },
            startTime.get(Calendar.HOUR_OF_DAY),
            startTime.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validateAndSave(): Boolean {
        binding.apply {
            val planName = etPlanName.text.toString().trim()
            if (planName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên kế hoạch", Toast.LENGTH_SHORT).show()
                return false
            }

            if (actvPlanType.text.toString().isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn loại kế hoạch", Toast.LENGTH_SHORT).show()
                return false
            }

            if (tvSelectedDate.text == "Chọn ngày") {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show()
                return false
            }

            if (tvStartTime.text == "Bắt đầu") {
                Toast.makeText(requireContext(), "Vui lòng chọn giờ bắt đầu", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validate within trip date range
            if (tripStartDate != null && startTime.before(tripStartDate)) {
                Toast.makeText(requireContext(), "Thời gian bắt đầu phải sau ngày bắt đầu chuyến đi", Toast.LENGTH_SHORT).show()
                return false
            }

            // FIXED: Check if startTime is AFTER tripEndDate
            if (tripEndDate != null && startTime.after(tripEndDate)) {
                Toast.makeText(requireContext(), "Thời gian bắt đầu phải trước ngày kết thúc chuyến đi", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validate no overlap with other plans (excluding current plan)
            val hasOverlap = existingPlans.any { plan ->
                if (plan.id == currentPlan.id) return@any false

                try {
                    val planStart = apiDateTimeFormat.parse(plan.startTime)?.time ?: 0
                    val newStart = startTime.timeInMillis

                    // Check if same start time (within 5 minutes)
                    Math.abs(newStart - planStart) < 5 * 60 * 1000
                } catch (e: Exception) {
                    false
                }
            }

            if (hasOverlap) {
                Toast.makeText(requireContext(), "Kế hoạch bị trùng thời gian với kế hoạch khác", Toast.LENGTH_SHORT).show()
                return false
            }

            // Get plan type
            @Suppress("UNCHECKED_CAST")
            val planTypes = actvPlanType.tag as List<Pair<String, PlanType>>
            val selectedTypeText = actvPlanType.text.toString()
            val planType = planTypes.firstOrNull { it.first == selectedTypeText }?.second ?: PlanType.ACTIVITY

            // Get cost
            val cost = etCost.text.toString().trim().toDoubleOrNull()

            // Create updated plan
            val updatedPlan = currentPlan.copy(
                title = planName,
                type = planType,
                startTime = apiDateTimeFormat.format(startTime.time),
                expense = cost
            )

            onSaveListener?.invoke(updatedPlan)
            return true
        }
    }

    fun setPlan(plan: AISuggestedPlan) {
        currentPlan = plan
    }

    fun setTripConstraints(startDate: String, endDate: String) {
        try {
            val tripFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            tripStartDate = Calendar.getInstance().apply {
                time = tripFormat.parse(startDate) ?: Date()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            tripEndDate = Calendar.getInstance().apply {
                time = tripFormat.parse(endDate) ?: Date()
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setExistingPlans(plans: List<AISuggestedPlan>) {
        existingPlans = plans
    }

    fun setOnSaveListener(listener: (plan: AISuggestedPlan) -> Unit) {
        onSaveListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            plan: AISuggestedPlan,
            tripStartDate: String,
            tripEndDate: String,
            existingPlans: List<AISuggestedPlan>
        ): EditPlanDialogFragment {
            return EditPlanDialogFragment().apply {
                setPlan(plan)
                setTripConstraints(tripStartDate, tripEndDate)
                setExistingPlans(existingPlans)
            }
        }
    }
}