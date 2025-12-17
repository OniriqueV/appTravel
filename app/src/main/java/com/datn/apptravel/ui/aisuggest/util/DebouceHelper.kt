package com.datn.apptravel.ui.aisuggest.util

import kotlinx.coroutines.*

/**
 * Helper class để xử lý debounce cho search
 * Giúp giảm số lượng API calls không cần thiết
 */
class DebouncedSearchHelper(
    private val delayMillis: Long = 500L,
    private val coroutineScope: CoroutineScope
) {
    private var searchJob: Job? = null

    fun search(action: suspend () -> Unit) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(delayMillis)
            action()
        }
    }

    fun cancel() {
        searchJob?.cancel()
    }
}

// Extension function cho EditText/AutoCompleteTextView
fun android.widget.EditText.onTextChangedDebounced(
    coroutineScope: CoroutineScope,
    delayMillis: Long = 500L,
    action: suspend (String) -> Unit
) {
    val helper = DebouncedSearchHelper(delayMillis, coroutineScope)

    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: android.text.Editable?) {
            val text = s.toString().trim()
            helper.search { action(text) }
        }
    })
}