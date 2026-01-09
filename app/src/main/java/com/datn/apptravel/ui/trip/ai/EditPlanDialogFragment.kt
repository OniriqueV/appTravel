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
    private var endTime: Calendar = Calendar.getInstance()

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

                val endDate = apiDateTimeFormat.parse(currentPlan.endTime)
                if (endDate != null) {
                    endTime.time = endDate
                    tvEndTime.text = timeFormat.format(endDate)
                    tvEndTime.setTextColor(resources.getColor(R.color.black, null))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Set cost
            currentPlan.expense?.takeIf { it > 0 }?.let {
                etCost.setText(it.toString())
            }

            // Set notes
            etNotes.setText(currentPlan.notes ?: "")
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnSelectDate.setOnClickListener {
                showDatePicker()
            }

            btnSelectStartTime.setOnClickListener {
                showTimePicker(true)
            }

            btnSelectEndTime.setOnClickListener {
                showTimePicker(false)
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

                // Update start and end time calendars with new date
                startTime.set(year, month, dayOfMonth)
                endTime.set(year, month, dayOfMonth)
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

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = if (isStartTime) startTime else endTime

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                if (isStartTime) {
                    binding.tvStartTime.text = timeFormat.format(calendar.time)
                    binding.tvStartTime.setTextColor(resources.getColor(R.color.black, null))
                } else {
                    binding.tvEndTime.text = timeFormat.format(calendar.time)
                    binding.tvEndTime.setTextColor(resources.getColor(R.color.black, null))
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
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

            if (tvEndTime.text == "Kết thúc") {
                Toast.makeText(requireContext(), "Vui lòng chọn giờ kết thúc", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validate end time is after start time
            if (endTime.timeInMillis <= startTime.timeInMillis) {
                Toast.makeText(requireContext(), "Giờ kết thúc phải sau giờ bắt đầu", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validate within trip date range
            if (tripStartDate != null && startTime.before(tripStartDate)) {
                Toast.makeText(requireContext(), "Thời gian nằm ngoài chuyến đi", Toast.LENGTH_SHORT).show()
                return false
            }

            if (tripEndDate != null && endTime.after(tripEndDate)) {
                Toast.makeText(requireContext(), "Thời gian nằm ngoài chuyến đi", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validate no overlap with other plans (excluding current plan)
            val hasOverlap = existingPlans.any { plan ->
                if (plan.id == currentPlan.id) return@any false

                try {
                    val planStart = apiDateTimeFormat.parse(plan.startTime)?.time ?: 0
                    val planEnd = apiDateTimeFormat.parse(plan.endTime)?.time ?: 0
                    val newStart = startTime.timeInMillis
                    val newEnd = endTime.timeInMillis

                    // Check for overlap
                    (newStart < planEnd && newEnd > planStart)
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
                endTime = apiDateTimeFormat.format(endTime.time),
                expense = cost,
                notes = etNotes.text.toString().trim().takeIf { it.isNotEmpty() }
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