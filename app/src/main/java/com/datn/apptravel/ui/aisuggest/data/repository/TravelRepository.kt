package com.datn.apptravel.ui.aisuggest.data.repository

import com.datn.apptravel.ui.aisuggest.data.api.ApiService
import com.datn.apptravel.ui.aisuggest.data.model.ApiResult
import com.datn.apptravel.ui.aisuggest.data.model.ChatRequest
import com.datn.apptravel.ui.aisuggest.data.model.Message
import com.datn.apptravel.ui.aisuggest.data.model.TravelRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.collections.firstOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.text.format
import kotlin.text.replace
import kotlin.text.trimIndent

class TravelRepository(private val apiService: ApiService) {

    suspend fun generateItinerary(travelRequest: TravelRequest): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(travelRequest)
                val chatRequest = ChatRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(
                        Message(
                            role = "system",
                            content = "Bạn là chuyên gia lập kế hoạch du lịch chuyên nghiệp. Hãy tạo lịch trình chi tiết, hấp dẫn và thực tế cho người dùng."
                        ),
                        Message(role = "user", content = prompt)
                    ),
                    maxTokens = 2000,
                    temperature = 0.7
                )

                val response = apiService.generateItinerary(chatRequest)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.error != null) {
                        ApiResult.Error("Lỗi API: ${body.error.message}")
                    } else {
                        val content = body?.choices?.firstOrNull()?.message?.content
                        if (content != null) {
                            ApiResult.Success(content)
                        } else {
                            ApiResult.Error("Không nhận được phản hồi từ AI")
                        }
                    }
                } else {
                    ApiResult.Error("Lỗi kết nối: ${response.code()} - ${response.message()}")
                }
            } catch (e: UnknownHostException) {
                ApiResult.Error("Không có kết nối mạng. Vui lòng kiểm tra Internet.")
            } catch (e: SocketTimeoutException) {
                ApiResult.Error("Kết nối timeout. Vui lòng thử lại.")
            } catch (e: Exception) {
                ApiResult.Error("Lỗi: ${e.message ?: "Không xác định"}")
            }
        }
    }

    private fun buildPrompt(req: TravelRequest): String {
        val interestsText = if (req.interests.isNotEmpty()) {
            req.interests.joinToString(", ")
        } else {
            "tham quan các điểm nổi tiếng"
        }

        return """
Tạo lịch trình du lịch chi tiết với các thông tin sau:

📍 Địa điểm: ${req.destination}
📅 Số ngày: ${req.days} ngày
💰 Ngân sách: ${formatMoney(req.budget)} VNĐ
👥 Số người: ${req.people} người
🎯 Sở thích: $interestsText

YÊU CẦU:
1. Lịch trình theo từng ngày (Ngày 1, Ngày 2,...)
2. Mỗi ngày bao gồm:
   - Buổi sáng, trưa, chiều, tối
   - Địa điểm cụ thể
   - Hoạt động gợi ý
   - Địa điểm ăn uống
   - Ước tính chi phí cho mỗi hoạt động
3. Tổng chi phí ước tính cuối cùng
4. Lưu ý và gợi ý hữu ích

Hãy viết rõ ràng, dễ đọc, có emoji và format đẹp.
        """.trimIndent()
    }

    private fun formatMoney(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }
}