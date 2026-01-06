package com.datn.apptravels.data.model

/**
 * Data class representing a language option
 */
data class Language(
    val name: String,
    val flagResource: Int,
    val isSelected: Boolean = false,
    val languageCode: String = name.lowercase() // Default language code
)