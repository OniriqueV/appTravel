package com.datn.apptravels.ui.profile.edit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.databinding.ActivityEditProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModel()
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(binding.ivAvatar)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
        loadUserProfile()
    }

    private fun setupUI() {
        // Back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Change avatar
        binding.ivAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnChangeAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserProfile() {
        viewModel.getUserProfile()
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(this) { user ->
            user?.let {
                // Load current data
                Glide.with(this)
                    .load(it.profilePicture)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.ivAvatar)

                binding.etFirstName.setText(it.firstName)
                binding.etLastName.setText(it.lastName)
            }
        }

        viewModel.updateResult.observe(this) { success ->
            if (success) {
                showToast("Cập nhật thành công")
                setResult(RESULT_OK)
                finish()
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                showToast(it)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Chọn từ thư viện", "Hủy")
        MaterialAlertDialogBuilder(this)
            .setTitle("Chọn ảnh đại diện")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveProfile() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.etFirstName.error = "Vui lòng nhập tên"
            return
        }

        if (lastName.isEmpty()) {
            binding.etLastName.error = "Vui lòng nhập họ"
            return
        }

        viewModel.updateProfile(firstName, lastName, selectedImageUri)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}