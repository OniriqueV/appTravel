package com.datn.apptravels.data.model

enum class TripTopic(val topicName: String, val iconRes: Int) {
    CUISINE("Cuisine", android.R.drawable.ic_menu_compass),
    DESTINATION("Destination", android.R.drawable.ic_menu_compass),
    ADVENTURE("Adventure", android.R.drawable.ic_menu_mylocation),
    RESORT("Resort", android.R.drawable.ic_menu_myplaces)
}

data class TopicSelection(
    val topic: TripTopic,
    var isSelected: Boolean = false
)
