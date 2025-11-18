package com.datn.apptravel.ui.discover.model

data class User(
    val userId: String,
    val username: String,
    val avatar: Int,
    val following: List<String> = emptyList()   // list userId mà mình follow
)
