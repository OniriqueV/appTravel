package com.datn.apptravels.ui.base

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel> : AppCompatActivity() {
    
    protected lateinit var binding: VB
    protected abstract val viewModel: VM

    abstract fun getViewBinding(): VB
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)
        
        setupUI()
        observeData()
    }
    

    open fun setupUI() {
        // Override in child classes
    }

    private fun observeData() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            handleLoading(isLoading)
        }
        
        // Observe error messages
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    open fun handleLoading(isLoading: Boolean) {
        // Override in child classes to show/hide loading indicators
    }

    open fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}