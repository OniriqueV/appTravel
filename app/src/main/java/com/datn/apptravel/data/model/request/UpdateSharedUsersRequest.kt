package com.datn.apptravel.data.model.request

data class UpdateSharedUsersRequest(
    val sharedWithUserIds: List<String>
)
