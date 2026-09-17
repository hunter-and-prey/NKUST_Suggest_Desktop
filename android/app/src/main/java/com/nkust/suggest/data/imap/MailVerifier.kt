package com.nkust.suggest.data.imap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Properties
import java.util.regex.Pattern
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.MimeMessage

class MailVerifier(
    private val host: String = "imap.gmail.com",
    private val port: Int = 993,
    private val username: String,
    passwordRaw: String
) {
    private val password = passwordRaw.replace(" ", "").trim()

    suspend fun waitForConfirmationUrl(
        timeoutSeconds: Int = 180,
        onProgress: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port.toString())
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.timeout", "15000")
            put("mail.imaps.connectiontimeout", "15000")
        }

        val session = Session.getInstance(props, null)
        val store = session.getStore("imaps")

        try {
            onProgress("正在連線至學校郵件伺服器 ($host)...")
            store.connect(host, port, username, password)
        } catch (e: Exception) {
            throw Exception("信箱登入失敗 (若為 Google 學生信箱，請使用 16 碼應用程式密碼): ${e.localizedMessage}")
        }

        try {
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)

            val startTime = System.currentTimeMillis()
            val deadline = startTime + (timeoutSeconds * 1000L)

            while (System.currentTimeMillis() < deadline) {
                val remain = ((deadline - System.currentTimeMillis()) / 1000).toInt()
                onProgress("正在信箱中搜尋確認信... (剩餘 $remain 秒)")

                val totalCount = inbox.messageCount
                if (totalCount > 0) {
                    val startMsg = (totalCount - 15).coerceAtLeast(1)
                    val messages = inbox.getMessages(startMsg, totalCount)

                    for (i in messages.indices.reversed()) {
                        val msg = messages[i]
                        val subject = msg.subject ?: ""
                        val from = msg.from?.joinToString { it.toString() } ?: ""

                        val isSuggest = subject.contains("建言") ||
                                subject.contains("NKUST") ||
                                subject.contains("校務") ||
                                from.contains("suggest") ||
                                from.contains("bboffice")

                        if (isSuggest) {
                            val body = extractBodyText(msg)
                            val urls = findConfirmationUrls(body)
                            if (urls.isNotEmpty()) {
                                onProgress("🎉 成功找到確認信！主旨: $subject")
                                return@withContext urls.last()
                            }
                        }
                    }
                }

                delay(3000)
            }

            throw Exception("搜尋逾時 ($timeoutSeconds 秒)，未收到確認信件，請檢查信箱或手動確認。")
        } finally {
            try {
                if (store.isConnected) store.close()
            } catch (_: Exception) {}
        }
    }

    private fun extractBodyText(part: Part): String {
        return try {
            if (part.isMimeType("text/plain")) {
                part.content.toString()
            } else if (part.isMimeType("text/html")) {
                part.content.toString()
            } else if (part.isMimeType("multipart/*")) {
                val mp = part.content as? Multipart ?: return ""
                val sb = StringBuilder()
                for (i in 0 until mp.count) {
                    sb.append(extractBodyText(mp.getBodyPart(i))).append("\n")
                }
                sb.toString()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun findConfirmationUrls(text: String): List<String> {
        val list = mutableListOf<String>()
        val regex = Pattern.compile("""https?://[^\s"'<>]+""")
        val matcher = regex.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            if (url.contains("suggest.nkust.edu.tw") &&
                (url.contains("Confirm") || url.contains("chk") || url.contains("Check") || url.contains("verify") || url.contains("Key="))
            ) {
                list.add(url)
            }
        }
        return list
    }
}
