package com.datn.apptravel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.datn.apptravel.databinding.FragmentOnboardingBinding
import com.datn.apptravel.ui.common.model.OnboardingData
import com.datn.apptravel.ui.auth.OnboardingViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class OnboardingFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingBinding
    private val viewModel: OnboardingViewModel by sharedViewModel()
    private var position: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        position = requireArguments().getInt(ARG_POSITION, 0)
        setupClicks()
        observeData()
    }

    private fun observeData() {
        viewModel.onboardingData.observe(viewLifecycleOwner) { dataList ->
            if (position in dataList.indices) {
                updateUI(dataList[position])
            }
        }
    }

    private fun updateUI(data: OnboardingData) {
        binding.imageOnboarding.setImageResource(data.imageRes)
        binding.textTitle.text = data.title
        binding.textDesc.text = data.description

        binding.btnBack.visibility = if (data.showBackButton) View.VISIBLE else View.GONE
        binding.btnNext.visibility = if (data.showNextButton) View.VISIBLE else View.GONE
        binding.btnStart.visibility = if (data.showStartButton) View.VISIBLE else View.GONE
    }

    // remove fade animation to avoid artifacts when swiping back

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { viewModel.navigateToPreviousPage() }
        binding.btnNext.setOnClickListener { viewModel.navigateToNextPage() }
        binding.btnStart.setOnClickListener {
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }
    }

    companion object {
        private const val ARG_POSITION = "position"
        fun newInstance(position: Int): OnboardingFragment = OnboardingFragment().apply {
            arguments = Bundle().apply { putInt(ARG_POSITION, position) }
        }
    }
}