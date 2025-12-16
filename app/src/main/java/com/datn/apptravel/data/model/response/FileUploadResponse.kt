package com.datn.apptravel.data.model.response

data class FileUploadResponse(
    val success: Boolean,
    val message: String,
    val fileName: String?,
    val fileUrl: String?
)