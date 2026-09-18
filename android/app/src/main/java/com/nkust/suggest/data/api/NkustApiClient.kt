package com.nkust.suggest.data.api

import com.nkust.suggest.data.model.Constants
import com.nkust.suggest.data.model.SubmitResult
import com.nkust.suggest.data.model.SuggestionPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class NkustApiClient {

    private val cookieStore = mutableListOf<Cookie>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                synchronized(cookieStore) {
                    cookieStore.addAll(cookies)
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return synchronized(cookieStore) {
                    cookieStore.toList()
                }
            }
        })
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCSRFToken(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(Constants.CREATE_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("連線校務系統失敗 (HTTP ${response.code})")
            }
            val html = response.body?.string() ?: ""
            val pattern1 = Pattern.compile("""name="__RequestVerificationToken"\s+type="hidden"\s+value="([^"]+)"""")
            val pattern2 = Pattern.compile("""value="([^"]+)"\s+name="__RequestVerificationToken"""")
            val pattern3 = Pattern.compile("""type="hidden"\s+name="__RequestVerificationToken"\s+value="([^"]+)"""")
            val pattern4 = Pattern.compile("""__RequestVerificationToken["'][^>]*value=["']([^"']+)["']""")

            for (p in listOf(pattern1, pattern2, pattern3, pattern4)) {
                val m = p.matcher(html)
                if (m.find()) {
                    val token = m.group(1) ?: ""
                    if (token.isNotEmpty()) return@withContext token
                }
            }

            throw IOException("無法在系統中擷取防偽權杖 (__RequestVerificationToken)")
        }
    }

    suspend fun submitSuggestion(payload: SuggestionPayload): SubmitResult = withContext(Dispatchers.IO) {
        try {
            val token = fetchCSRFToken()

            // 核心關鍵修復：ASP.NET MVC 防偽權杖驗證
            // 權杖包含 CookieToken 與 FormToken，以冒號分隔 (CookieToken:FormToken)
            // 伺服器必須接收到名為 __RequestVerificationToken 的 Cookie，否則會丟出 HttpAntiForgeryException (HTTP 500)
            val tokenParts = token.split(":")
            val cookieToken = tokenParts[0]

            val targetHttpUrl = Constants.CREATE_URL.toHttpUrl()
            val csrfCookie = Cookie.Builder()
                .name("__RequestVerificationToken")
                .value(cookieToken)
                .domain(targetHttpUrl.host)
                .path("/")
                .build()

            synchronized(cookieStore) {
                cookieStore.removeAll { it.name == "__RequestVerificationToken" }
                cookieStore.add(csrfCookie)
            }

            val emptyFileBody = ByteArray(0).toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val formBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("__RequestVerificationToken", token)
                .addFormDataPart("Name", payload.name.trim())
                .addFormDataPart("GuestType", payload.guestType)
                .addFormDataPart("Email", payload.email.trim())
                .addFormDataPart("Phone", payload.phone.trim())
                .addFormDataPart("UnitId", payload.unitId)
                .addFormDataPart("Subject", payload.subject.trim())
                .addFormDataPart("MessageContent", payload.content)
                .addFormDataPart("SecrecyType", payload.secrecyType)
                .addFormDataPart("DoneOpen", payload.doneOpen)

            // ASP.NET MVC 模型繫結規格：補齊 3 個空的 files 欄位
            for (i in 0 until 3) {
                formBodyBuilder.addFormDataPart("files", "", emptyFileBody)
            }

            val request = Request.Builder()
                .url(Constants.CREATE_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", Constants.CREATE_URL)
                .header("__RequestVerificationToken", token)
                .header("Cookie", "__RequestVerificationToken=$cookieToken")
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
                    if (response.isSuccessful && (respString.contains("成功") || respString.contains("已建立"))) {
                        SubmitResult(true, "建言已提交！請收取信箱確認信。")
                    } else {
                        SubmitResult(false, "伺服器回應異常 (HTTP ${response.code}): ${respString.take(120)}")
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
