package com.datn.apptravel.ui.aisuggest.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.datn.apptravel.BuildConfig
import com.datn.apptravel.ui.aisuggest.data.api.ApiService
import com.datn.apptravel.ui.aisuggest.data.api.GeoapifyClient
import com.datn.apptravel.ui.aisuggest.data.api.GeoapifyService
import com.datn.apptravel.ui.aisuggest.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExtendedTravelRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("travel_prefs", Context.MODE_PRIVATE)
    private val geoapifyService: GeoapifyService = GeoapifyClient.create()

    private val conversationHistory = mutableListOf<ChatMessage>()
    private var currentItinerary: String? = null

    // Cache cho tìm kiếm địa điểm
    private val searchCache = mutableMapOf<String, Pair<List<String>, Long>>()
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 phút

    // 1. TÌM KIẾM ĐỊA ĐIỂM THẬT VỚI CACHE VÀ FUZZY MATCHING (TOÀN CẦU)
    suspend fun searchPlaces(query: String): ApiResult<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedQuery = normalizeVietnamese(query)

                // Kiểm tra cache với normalized query
                val cached = searchCache[normalizedQuery]
                val now = System.currentTimeMillis()

                if (cached != null && (now - cached.second) < CACHE_DURATION) {
                    return@withContext ApiResult.Success(cached.first)
                }

                // Gọi API - không có filter để tìm toàn cầu
                val response = geoapifyService.searchPlaces(
                    query = query,
                    apiKey = BuildConfig.GEOAPIFY_API_KEY,
                    filter = null, // Null = tìm toàn thế giới
                    type = "city,country" // Chỉ tìm thành phố và quốc gia
                )

                if (response.isSuccessful) {
                    val places = response.body()?.features
                        ?.mapNotNull { feature ->
                            val props = feature.properties

                            // Format: "Thành phố, Quốc gia"
                            val city = props?.city ?: props?.name
                            val country = props?.country

                            when {
                                !city.isNullOrBlank() && !country.isNullOrBlank() -> {
                                    if (city == country) city else "$city, $country"
                                }
                                !props?.formatted.isNullOrBlank() -> props?.formatted
                                !city.isNullOrBlank() -> city
                                else -> null
                            }
                        }
                        ?.distinct()
                        ?.filter { it.isNotBlank() }
                        ?.sortedBy { place ->
                            // Sắp xếp: Việt Nam lên đầu, sau đó theo độ match
                            val normalized = normalizeVietnamese(place)
                            val isVietnam = place.contains("Việt Nam", ignoreCase = true) ||
                                    place.contains("Vietnam", ignoreCase = true)

                            when {
                                isVietnam && normalized.equals(normalizedQuery, ignoreCase = true) -> 0
                                isVietnam && normalized.startsWith(normalizedQuery, ignoreCase = true) -> 1
                                isVietnam -> 2
                                normalized.equals(normalizedQuery, ignoreCase = true) -> 3
                                normalized.startsWith(normalizedQuery, ignoreCase = true) -> 4
                                normalized.contains(normalizedQuery, ignoreCase = true) -> 5
                                else -> 6
                            }
                        }
                        ?.take(15) // Tăng lên 15 kết quả cho quốc tế
                        ?: emptyList()

                    // Lưu vào cache
                    searchCache[normalizedQuery] = Pair(places, now)

                    ApiResult.Success(places)
                } else {
                    ApiResult.Error("Lỗi tìm kiếm: ${response.code()}")
                }
            } catch (e: Exception) {
                ApiResult.Error("Lỗi: ${e.message}")
            }
        }
    }

    // Helper: normalize tiếng Việt để tìm kiếm tốt hơn
    private fun normalizeVietnamese(text: String): String {
        return text.trim().lowercase()
            .replace("đ", "d")
            .replace("  ", " ")
    }

    // 2. TẠO LỊCH TRÌNH BAN ĐẦU
    suspend fun generateItinerary(travelRequest: TravelRequest): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(travelRequest)
                val chatRequest = ChatRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(
                        Message(
                            role = "system",
                            content = "Bạn là chuyên gia lập kế hoạch du lịch. Hãy tạo lịch trình chi tiết, thực tế."
                        ),
                        Message(role = "user", content = prompt)
                    ),
                    maxTokens = 2000,
                    temperature = 0.7
                )

                val response = apiService.generateItinerary(chatRequest)

                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        // Lưu lịch trình hiện tại
                        currentItinerary = content
                        // Khởi tạo lịch sử chat
                        conversationHistory.clear()
                        conversationHistory.add(ChatMessage("assistant", content))

                        ApiResult.Success(content)
                    } else {
                        ApiResult.Error("Không nhận được phản hồi")
                    }
                } else {
                    ApiResult.Error("Lỗi: ${response.code()}")
                }
            } catch (e: Exception) {
                ApiResult.Error("Lỗi: ${e.message}")
            }
        }
    }

    // 3. CHAT TƯƠNG TÁC SỬA LỊCH TRÌNH
    suspend fun chatToModifyItinerary(userMessage: String): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (currentItinerary == null) {
                    return@withContext ApiResult.Error("Chưa có lịch trình nào để chỉnh sửa")
                }

                // Thêm tin nhắn người dùng
                conversationHistory.add(ChatMessage("user", userMessage))

                // Tạo context từ lịch sử chat
                val messages = mutableListOf<Message>()
                messages.add(Message(
                    role = "system",
                    content = """Bạn là trợ lý du lịch. Dựa vào lịch trình hiện tại, hãy điều chỉnh theo yêu cầu của người dùng.
                    
LỊCH TRÌNH HIỆN TẠI:
$currentItinerary

Hãy trả lời ngắn gọn, chỉ nêu những thay đổi hoặc đưa ra lịch trình mới nếu cần."""
                ))

                // Thêm lịch sử chat gần đây (5 tin nhắn cuối)
                conversationHistory.takeLast(5).forEach {
                    messages.add(Message(role = it.role, content = it.content))
                }

                val chatRequest = ChatRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = messages,
                    maxTokens = 1500,
                    temperature = 0.7
                )

                val response = apiService.generateItinerary(chatRequest)

                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        // Cập nhật lịch trình nếu có thay đổi lớn
                        if (content.contains("Ngày 1") || content.length > 500) {
                            currentItinerary = content
                        }

                        conversationHistory.add(ChatMessage("assistant", content))
                        ApiResult.Success(content)
                    } else {
                        ApiResult.Error("Không nhận được phản hồi")
                    }
                } else {
                    ApiResult.Error("Lỗi: ${response.code()}")
                }
            } catch (e: Exception) {
                ApiResult.Error("Lỗi: ${e.message}")
            }
        }
    }

    // 4. LƯU LỊCH TRÌNH
    fun saveItinerary(itinerary: SavedItinerary): Boolean {
        return try {
            val savedList = getSavedItineraries().toMutableList()
            savedList.add(0, itinerary) // Thêm vào đầu danh sách

            val json = JsonHelper.toJson(savedList)
            prefs.edit().putString("saved_itineraries", json).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 5. LẤY DANH SÁCH LỊCH TRÌNH ĐÃ LƯU
    fun getSavedItineraries(): List<SavedItinerary> {
        return try {
            val json = prefs.getString("saved_itineraries", null) ?: return emptyList()
            JsonHelper.fromJson<List<SavedItinerary>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 6. XÓA LỊCH TRÌNH
    fun deleteItinerary(id: String): Boolean {
        return try {
            val savedList = getSavedItineraries().toMutableList()
            savedList.removeAll { it.id == id }

            val json = JsonHelper.toJson(savedList)
            prefs.edit().putString("saved_itineraries", json).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 7. LẤY LỊCH TRÌNH HIỆN TẠI
    fun getCurrentItinerary(): String? = currentItinerary

    // 8. LẤY LỊCH SỬ CHAT
    fun getChatHistory(): List<ChatMessage> = conversationHistory.toList()

    private fun buildPrompt(req: TravelRequest): String {
        val interestsText = if (req.interests.isNotEmpty()) {
            req.interests.joinToString(", ")
        } else {
            "tham quan các điểm nổi tiếng"
        }

        // Xác định có phải địa điểm Việt Nam không
        val isVietnam = req.destination.contains("Vietnam", ignoreCase = true) ||
                req.destination.contains("Việt Nam", ignoreCase = true) ||
                listOf("Hà Nội", "Sài Gòn", "Đà Nẵng", "Nha Trang", "Phú Quốc",
                    "Đà Lạt", "Hội An", "Huế", "Sapa", "Hạ Long")
                    .any { req.destination.contains(it, ignoreCase = true) }

        return """
Tạo lịch trình du lịch chi tiết:

📍 Địa điểm: ${req.destination}
📅 Số ngày: ${req.days} ngày
💰 Ngân sách: ${formatMoney(req.budget)} ${if (isVietnam) "VNĐ" else "VNĐ (≈ ${formatUSD(req.budget)} USD)"}
👥 Số người: ${req.people} người
🎯 Sở thích: $interestsText

YÊU CẦU:
1. Lịch trình theo từng ngày (Ngày 1, Ngày 2,...)
2. Mỗi ngày bao gồm:
   - Buổi sáng, trưa, chiều, tối
   - Địa điểm cụ thể (tên tiếng địa phương + tiếng Việt nếu có)
   - Hoạt động gợi ý
   - Địa điểm ăn uống đặc trưng
   - Chi phí ước tính (${if (isVietnam) "VNĐ" else "đơn vị tiền tệ địa phương + VNĐ"})
3. ${if (!isVietnam) "Lưu ý về visa, tiền tệ, ngôn ngữ, văn hóa địa phương\n4. " else ""}Tổng chi phí ước tính cuối cùng
${if (!isVietnam) "5." else "4."} Lưu ý và gợi ý hữu ích

${if (!isVietnam) "LƯU Ý ĐẶC BIỆT: Đây là lịch trình du lịch quốc tế, hãy bao gồm thông tin về:\n- Đổi tiền tệ\n- Giao tiếp cơ bản\n- Phong tục địa phương\n- Gợi ý di chuyển nội địa\n\n" else ""}Hãy viết rõ ràng, dễ đọc, có emoji và format đẹp.
        """.trimIndent()
    }

    private fun formatMoney(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }

    private fun formatUSD(amountVND: Long): String {
        val usd = amountVND / 24000 // Tỷ giá gần đúng 1 USD = 24,000 VNĐ
        return String.format("%,d", usd).replace(",", ".")
    }
}