package com.datn.apptravel.ui.trip.ai

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.datn.apptravel.R
import com.datn.apptravel.data.model.UserInsight
import com.datn.apptravel.databinding.DialogAiInsightBinding
import com.google.android.material.chip.Chip

/**
 * Dialog hỏi insights người dùng trước khi AI generate
 */
class AIInsightDialogFragment : DialogFragment() {

    private var _binding: DialogAiInsightBinding? = null
    private val binding get() = _binding!!

    private var onResultListener: ((UserInsight?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiInsightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Skip button - không trả lời câu hỏi
        binding.btnSkip.setOnClickListener {
            onResultListener?.invoke(null)
            dismiss()
        }

        // Continue button - lấy insights
        binding.btnContinue.setOnClickListener {
            val insight = collectUserInsight()
            onResultListener?.invoke(insight)
            dismiss()
        }
    }

    private fun collectUserInsight(): UserInsight {
        // Budget
        val budget = when (binding.rgBudget.checkedRadioButtonId) {
            R.id.rbLowBudget -> "LOW"
            R.id.rbMediumBudget -> "MEDIUM"
            R.id.rbHighBudget -> "HIGH"
            else -> null
        }

        // Travel style
        val travelStyle = when (binding.rgTravelStyle.checkedRadioButtonId) {
            R.id.rbRelaxed -> "RELAXED"
            R.id.rbBalanced -> "BALANCED"
            R.id.rbPacked -> "PACKED"
            else -> null
        }

        // Interests (from chips)
        val interests = mutableListOf<String>()
        binding.chipGroupInterests.children.forEach { view ->
            if (view is Chip && view.isChecked) {
                interests.add(view.tag as String)
            }
        }

        // Group size
        val groupSize = binding.etGroupSize.text.toString().toIntOrNull()

        // Has children
        val hasChildren = binding.switchHasChildren.isChecked

        // Preferred cuisine (from chips)
        val cuisine = mutableListOf<String>()
        binding.chipGroupCuisine.children.forEach { view ->
            if (view is Chip && view.isChecked) {
                cuisine.add(view.tag as String)
            }
        }

        // Accommodation type
        val accommodation = when (binding.rgAccommodation.checkedRadioButtonId) {
            R.id.rbHotel -> "HOTEL"
            R.id.rbHostel -> "HOSTEL"
            R.id.rbApartment -> "APARTMENT"
            else -> null
        }

        return UserInsight(
            budget = budget,
            travelStyle = travelStyle,
            interests = interests,
            groupSize = groupSize,
            hasChildren = hasChildren,
            preferredCuisine = cuisine,
            accommodationType = accommodation
        )
    }

    fun setOnResultListener(listener: (UserInsight?) -> Unit) {
        this.onResultListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AIInsightDialogFragment {
            return AIInsightDialogFragment()
        }
    }
}