package com.datn.apptravels.data.model.response

data class FileUploadResponse(
    val success: Boolean,
    val message: String,
    val fileName: String?,
    val fileUrl: String?
)