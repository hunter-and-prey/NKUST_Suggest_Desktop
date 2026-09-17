package com.nkust.suggest.data.api

import com.nkust.suggest.data.model.Constants
import com.nkust.suggest.data.model.SubmitResult
import com.nkust.suggest.data.model.SuggestionPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class NkustApiClient {

    private val cookieStore = mutableListOf<Cookie>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore.addAll(cookies)
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore
            }
        })
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCSRFToken(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(Constants.CREATE_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("連線校務系統失敗 (HTTP ${response.code})")
            }
            val html = response.body?.string() ?: ""
            val pattern1 = Pattern.compile("""name="__RequestVerificationToken"\s+type="hidden"\s+value="([^"]+)"""")
            val pattern2 = Pattern.compile("""value="([^"]+)"\s+name="__RequestVerificationToken"""")
            val pattern3 = Pattern.compile("""__RequestVerificationToken["'][^>]*value=["']([^"']+)["']""")

            val m1 = pattern1.matcher(html)
            if (m1.find()) return@withContext m1.group(1) ?: ""

            val m2 = pattern2.matcher(html)
            if (m2.find()) return@withContext m2.group(1) ?: ""

            val m3 = pattern3.matcher(html)
            if (m3.find()) return@withContext m3.group(1) ?: ""

            throw IOException("無法在系統中擷取防偽權杖 (__RequestVerificationToken)")
        }
    }

    suspend fun submitSuggestion(payload: SuggestionPayload): SubmitResult = withContext(Dispatchers.IO) {
        try {
            val token = fetchCSRFToken()

            val formBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("__RequestVerificationToken", token)
                .addFormDataPart("GuestName", payload.name)
                .addFormDataPart("GuestType", payload.guestType)
                .addFormDataPart("GuestEmail", payload.email)
                .addFormDataPart("GuestTel", payload.phone)
                .addFormDataPart("UnitId", payload.unitId)
                .addFormDataPart("Subject", payload.subject)
                .addFormDataPart("Msg", payload.content)
                .addFormDataPart("Secrecy", payload.secrecyType)
                .addFormDataPart("DoneOpen", payload.doneOpen)

            val request = Request.Builder()
                .url(Constants.CREATE_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(formBodyBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                
                try {
                    val json = JSONObject(respString)
                    val result = json.optBoolean("result", false)
                    val msg = json.optString("message", "")

                    if (result) {
                        SubmitResult(
                            success = true,
                            message = if (msg.isNotEmpty()) msg else "建言已成功送出！確認信已發送至您的信箱。"
                        )
                    } else {
                        SubmitResult(
                            success = false,
                            message = if (msg.isNotEmpty()) msg else "送出失敗，請確認資料填寫是否齊全。"
                        )
                    }
                } catch (e: Exception) {
                    if (response.isSuccessful) {
                        SubmitResult(true, "建言已提交！請收取信箱確認信。")
                    } else {
                        SubmitResult(false, "伺服器回應異常: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            SubmitResult(false, "送出失敗: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun clickConfirmationUrl(url: String): SubmitResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SubmitResult(true, "🎉 建言確認信已自動點擊確認完成！您的校務建言已正式生效！")
                } else {
                    SubmitResult(false, "確認信連結觸發失敗 (HTTP ${response.code})")
                }
            }
        } catch (e: Exception) {
            SubmitResult(false, "點擊確認信失敗: ${e.localizedMessage ?: e.message}")
        }
    }
}
