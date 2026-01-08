package com.datn.apptravels.data.model

data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconRes: Int = 0, // Resource ID for badge icon
    val condition: BadgeCondition = BadgeCondition.TRIPS_CREATED,
    val threshold: Int = 0, // Số lượng cần đạt để nhận huy hiệu
    val category: BadgeCategory = BadgeCategory.EXPLORER
) {
    constructor() : this("", "", "", 0, BadgeCondition.TRIPS_CREATED, 0)
}

data class UserBadge(
    val userId: String = "",
    val badgeId: String = "",
    val badgeName: String = "",
    val badgeDescription: String = "",
    val badgeIconRes: Int = 0,
    val earnedAt: Long = System.currentTimeMillis(),
    val isNew: Boolean = true // Để hiển thị "New" badge
) {
    constructor() : this("", "", "", "", 0)

    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "userId" to userId,
            "badgeId" to badgeId,
            "badgeName" to badgeName,
            "badgeDescription" to badgeDescription,
            "badgeIconRes" to badgeIconRes,
            "earnedAt" to earnedAt,
            "isNew" to isNew
        )
    }
}

enum class BadgeCondition {
    TRIPS_CREATED,      // Số chuyến đi đã tạo
    PLANS_COMPLETED,    // Số kế hoạch hoàn thành
    DOCUMENTS_UPLOADED, // Số tài liệu đã tải lên
    DAYS_TRAVELED,      // Tổng số ngày du lịch
    COUNTRIES_VISITED   // Số quốc gia đã đi
}

enum class BadgeCategory {
    EXPLORER,    // Người khám phá
    PLANNER,     // Người lập kế hoạch
    ORGANIZER,   // Người tổ chức
    TRAVELER     // Nhà du lịch
}

// Predefined badges
object BadgeDefinitions {
    fun getAllBadges(): List<Badge> {
        return listOf(
            // Explorer Badges
            Badge(
                id = "first_trip",
                name = "First Journey",
                description = "Tạo chuyến đi đầu tiên",
                iconRes = 0, // R.drawable.badge_first_trip
                condition = BadgeCondition.TRIPS_CREATED,
                threshold = 1,
                category = BadgeCategory.EXPLORER
            ),
            Badge(
                id = "trip_master_5",
                name = "Trip Master",
                description = "Tạo 5 chuyến đi",
                iconRes = 0,
                condition = BadgeCondition.TRIPS_CREATED,
                threshold = 5,
                category = BadgeCategory.EXPLORER
            ),
            Badge(
                id = "globe_trotter_10",
                name = "Globe Trotter",
                description = "Tạo 10 chuyến đi",
                iconRes = 0,
                condition = BadgeCondition.TRIPS_CREATED,
                threshold = 10,
                category = BadgeCategory.EXPLORER
            ),

            // Planner Badges
            Badge(
                id = "organized_planner_10",
                name = "Organized Planner",
                description = "Tạo 10 kế hoạch",
                iconRes = 0,
                condition = BadgeCondition.PLANS_COMPLETED,
                threshold = 10,
                category = BadgeCategory.PLANNER
            ),
            Badge(
                id = "master_planner_50",
                name = "Master Planner",
                description = "Tạo 50 kế hoạch",
                iconRes = 0,
                condition = BadgeCondition.PLANS_COMPLETED,
                threshold = 50,
                category = BadgeCategory.PLANNER
            ),

            // Organizer Badges
            Badge(
                id = "document_keeper_5",
                name = "Document Keeper",
                description = "Tải lên 5 tài liệu",
                iconRes = 0,
                condition = BadgeCondition.DOCUMENTS_UPLOADED,
                threshold = 5,
                category = BadgeCategory.ORGANIZER
            ),
            Badge(
                id = "archive_master_20",
                name = "Archive Master",
                description = "Tải lên 20 tài liệu",
                iconRes = 0,
                condition = BadgeCondition.DOCUMENTS_UPLOADED,
                threshold = 20,
                category = BadgeCategory.ORGANIZER
            )
        )
    }
}