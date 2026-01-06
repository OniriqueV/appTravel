package com.datn.apptravel.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ExpenseFormatter {
    

    fun formatExpense(expense: Double): String {
        if (expense == 0.0) return "0"
        
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(expense)
    }
    

    fun formatExpenseWithCurrency(expense: Double): String {
        if (expense == 0.0) return "0đ"
        return "${formatExpense(expense)}đ"
    }

    fun parseExpense(formattedExpense: String): Double {
        return try {
            // Remove all dots and parse
            val cleanString = formattedExpense.replace(".", "").replace(",", "").trim()
            if (cleanString.isEmpty()) 0.0 else cleanString.toDouble()
        } catch (e: Exception) {
            0.0
        }
    }
    

    fun addExpenseFormatter(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private var previousText = ""
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Store previous text
                if (!isFormatting) {
                    previousText = s?.toString() ?: ""
                }
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                
                try {
                    val input = s?.toString() ?: ""
                    
                    // Remove all dots to get raw number
                    val cleanString = input.replace(".", "")
                    
                    if (cleanString.isEmpty()) {
                        editText.setText("")
                        editText.setSelection(0)
                        return
                    }
                    
                    // Parse to Long to avoid floating point issues
                    val parsed = cleanString.toLongOrNull()
                    if (parsed != null) {
                        // Format with thousand separators
                        val formatted = formatExpense(parsed.toDouble())
                        
                        // Only update if different to avoid infinite loop
                        if (formatted != input) {
                            // Calculate new cursor position
                            val originalCursorPos = editText.selectionStart
                            val originalDotsBeforeCursor = input.substring(0, originalCursorPos.coerceAtMost(input.length)).count { it == '.' }
                            
                            editText.setText(formatted)
                            
                            // Restore cursor position, accounting for added/removed dots
                            val newDotsBeforeCursor = formatted.substring(0, originalCursorPos.coerceAtMost(formatted.length)).count { it == '.' }
                            val cursorOffset = newDotsBeforeCursor - originalDotsBeforeCursor
                            val newCursorPos = (originalCursorPos + cursorOffset).coerceIn(0, formatted.length)
                            
                            editText.setSelection(newCursorPos)
                        }
                    } else {
                        // Invalid number, restore previous text
                        editText.setText(previousText)
                        editText.setSelection(previousText.length)
                    }
                } catch (e: Exception) {
                    // On error, restore previous text
                    editText.setText(previousText)
                    editText.setSelection(previousText.length)
                } finally {
                    isFormatting = false
                }
            }
        })
    }
}
