package com.datn.apptravels.ui.trip.ai

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.datn.apptravels.R
import com.datn.apptravels.data.model.CityPlan
import com.datn.apptravels.databinding.DialogAiSimpleInputBinding
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AIDialogFragment : DialogFragment() {

    private var _binding: DialogAiSimpleInputBinding? = null
    private val binding get() = _binding!!

    private val cityPlans = mutableListOf<CityPlanItem>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val tripDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var onResultListener: ((cities: List<CityPlan>) -> Unit)? = null
    
    // Trip date range for validation
    private var tripStartDate: Calendar? = null
    private var tripEndDate: Calendar? = null

    data class CityPlanItem(
        val view: View,
        var cityName: String = "",
        var startDate: Calendar? = null,
        var endDate: Calendar? = null,
        var budget: Long? = null,
        var numberOfPlans: Int? = null,
        var isExpanded: Boolean = false
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiSimpleInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Set dialog size
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Set transparent background to show rounded corners
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add first city by default
        addCityPlan()
        
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnAddCity.setOnClickListener {
            addCityPlan()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnGenerate.setOnClickListener {
            if (validateAndGenerate()) {
                val plans = cityPlans.mapNotNull { item ->
                    // Update values from EditText before creating CityPlan
                    val etCityName = item.view.findViewById<TextInputEditText>(R.id.etCityName)
                    val etBudget = item.view.findViewById<TextInputEditText>(R.id.etBudget)
                    val etNumberOfPlans = item.view.findViewById<TextInputEditText>(R.id.etNumberOfPlans)
                    
                    item.cityName = etCityName.text.toString().trim()
                    item.budget = etBudget.text.toString().trim().toLongOrNull()
                    item.numberOfPlans = etNumberOfPlans.text.toString().trim().toIntOrNull()
                    
                    if (item.cityName.isNotEmpty() && item.startDate != null && item.endDate != null) {
                        CityPlan(
                            cityName = item.cityName,
                            startDate = apiDateFormat.format(item.startDate!!.time),
                            endDate = apiDateFormat.format(item.endDate!!.time),
                            budget = item.budget,
                            numberOfPlans = item.numberOfPlans
                        )
                    } else null
                }
                onResultListener?.invoke(plans)
                dismiss()
            }
        }
    }

    private fun addCityPlan() {
        val itemView = layoutInflater.inflate(R.layout.item_city_plan, binding.containerCities, false)
        
        val etCityName = itemView.findViewById<TextInputEditText>(R.id.etCityName)
        val tvStartDate = itemView.findViewById<TextView>(R.id.tvStartDate)
        val tvEndDate = itemView.findViewById<TextView>(R.id.tvEndDate)
        val btnStartDate = itemView.findViewById<LinearLayout>(R.id.btnStartDate)
        val btnEndDate = itemView.findViewById<LinearLayout>(R.id.btnEndDate)
        val btnRemoveCity = itemView.findViewById<ImageButton>(R.id.btnRemoveCity)
        val btnExpandDetails = itemView.findViewById<ImageButton>(R.id.btnExpandDetails)
        val layoutExpandedDetails = itemView.findViewById<LinearLayout>(R.id.layoutExpandedDetails)
        val etBudget = itemView.findViewById<TextInputEditText>(R.id.etBudget)
        val etNumberOfPlans = itemView.findViewById<TextInputEditText>(R.id.etNumberOfPlans)

        val cityPlanItem = CityPlanItem(view = itemView)
        cityPlans.add(cityPlanItem)

        // Show/hide remove button
        btnRemoveCity.visibility = if (cityPlans.size > 1) View.VISIBLE else View.GONE

        // Expand/Collapse details
        btnExpandDetails.setOnClickListener {
            cityPlanItem.isExpanded = !cityPlanItem.isExpanded
            layoutExpandedDetails.visibility = if (cityPlanItem.isExpanded) View.VISIBLE else View.GONE
            btnExpandDetails.setImageResource(
                if (cityPlanItem.isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            )
        }

        // City name input
        etCityName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                cityPlanItem.cityName = etCityName.text.toString().trim()
            }
        }

        // Budget input
        etBudget.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val budgetText = etBudget.text.toString().trim()
                cityPlanItem.budget = if (budgetText.isNotEmpty()) budgetText.toLongOrNull() else null
            }
        }

        // Number of plans input
        etNumberOfPlans.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val plansText = etNumberOfPlans.text.toString().trim()
                cityPlanItem.numberOfPlans = if (plansText.isNotEmpty()) plansText.toIntOrNull() else null
            }
        }

        // Start date picker
        btnStartDate.setOnClickListener {
            showDatePicker(cityPlanItem, true, tvStartDate)
        }

        // End date picker
        btnEndDate.setOnClickListener {
            showDatePicker(cityPlanItem, false, tvEndDate)
        }

        // Remove city
        btnRemoveCity.setOnClickListener {
            removeCityPlan(cityPlanItem)
        }

        binding.containerCities.addView(itemView)
        
        // Scroll to bottom
        binding.scrollCities.post {
            binding.scrollCities.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun removeCityPlan(item: CityPlanItem) {
        binding.containerCities.removeView(item.view)
        cityPlans.remove(item)
        
        // Update remove button visibility
        if (cityPlans.size == 1) {
            val firstItem = cityPlans[0]
            firstItem.view.findViewById<ImageButton>(R.id.btnRemoveCity).visibility = View.GONE
        }
    }

    private fun showDatePicker(item: CityPlanItem, isStartDate: Boolean, textView: TextView) {
        val calendar = if (isStartDate) item.startDate ?: Calendar.getInstance() 
                       else item.endDate ?: item.startDate ?: Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (isStartDate) {
                    item.startDate = selectedCalendar
                    textView.text = dateFormat.format(selectedCalendar.time)
                } else {
                    // Validate end date is after start date
                    if (item.startDate != null && selectedCalendar.before(item.startDate)) {
                        Toast.makeText(requireContext(), "Ngày về phải sau ngày đến", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    item.endDate = selectedCalendar
                    textView.text = dateFormat.format(selectedCalendar.time)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Only set minDate for end date picker based on start date
            if (!isStartDate && item.startDate != null) {
                datePicker.minDate = item.startDate!!.timeInMillis
            }
            // Allow selecting any date in calendar - validation happens on generate
        }.show()
    }

    private fun validateAndGenerate(): Boolean {
        if (cityPlans.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng thêm ít nhất 1 thành phố", Toast.LENGTH_SHORT).show()
            return false
        }

        for ((index, item) in cityPlans.withIndex()) {
            val etCityName = item.view.findViewById<TextInputEditText>(R.id.etCityName)
            item.cityName = etCityName.text.toString().trim()

            if (item.cityName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên thành phố ${index + 1}", Toast.LENGTH_SHORT).show()
                return false
            }

            if (item.startDate == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày đến cho ${item.cityName}", Toast.LENGTH_SHORT).show()
                return false
            }

            if (item.endDate == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày về cho ${item.cityName}", Toast.LENGTH_SHORT).show()
                return false
            }
            
            // Final validation: ensure dates are within trip range
            if (tripStartDate != null && item.startDate!!.before(tripStartDate)) {
                Toast.makeText(requireContext(), "Ngoài thời gian chuyến đi. Người dùng phải nhập đúng trong thời gian chuyến đi", Toast.LENGTH_LONG).show()
                return false
            }
            
            if (tripEndDate != null && item.endDate!!.after(tripEndDate)) {
                Toast.makeText(requireContext(), "Ngoài thời gian chuyến đi. Người dùng phải nhập đúng trong thời gian chuyến đi", Toast.LENGTH_LONG).show()
                return false
            }
        }

        return true
    }

    fun setOnResultListener(listener: (cities: List<CityPlan>) -> Unit) {
        onResultListener = listener
    }
    
    fun setTripDateRange(startDate: String, endDate: String) {
        try {
            tripStartDate = Calendar.getInstance().apply {
                time = tripDateFormat.parse(startDate) ?: Date()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            tripEndDate = Calendar.getInstance().apply {
                time = tripDateFormat.parse(endDate) ?: Date()
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AIDialogFragment {
            return AIDialogFragment()
        }
        
        fun newInstance(tripStartDate: String, tripEndDate: String): AIDialogFragment {
            return AIDialogFragment().apply {
                setTripDateRange(tripStartDate, tripEndDate)
            }
        }
    }
}
