package com.nkust.suggest.data.imap

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Properties
import java.util.regex.Pattern
import javax.activation.CommandMap
import javax.activation.MailcapCommandMap
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility

class MailVerifier(
    private val host: String = "imap.gmail.com",
    private val port: Int = 993,
    private val username: String,
    passwordRaw: String
) {
    private val password = passwordRaw.replace(" ", "").trim()

    init {
        // 核心修復 1：初始化 Android Activation CommandMap，防止 JAF 缺失拋出 UnsupportedDataTypeException
        try {
            val mc = CommandMap.getDefaultCommandMap() as? MailcapCommandMap ?: MailcapCommandMap()
            mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
            mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
            mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
            mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
            mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
            CommandMap.setDefaultCommandMap(mc)
        } catch (e: Exception) {
            Log.w("MailVerifier", "Failed to init MailcapCommandMap: ${e.message}")
        }
    }

    suspend fun waitForConfirmationUrl(
        timeoutSeconds: Int = 90,
        onProgress: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port.toString())
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.timeout", "15000")
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.partialfetch", "false")
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
            val startTime = System.currentTimeMillis()
            val deadline = startTime + (timeoutSeconds * 1000L)

            while (System.currentTimeMillis() < deadline) {
                val remain = ((deadline - System.currentTimeMillis()) / 1000).toInt()
                onProgress("正在信箱中搜尋確認信... (剩餘 $remain 秒)")

                // 核心修復 2：每次輪詢重新取得並打開 INBOX 資料夾，強制 IMAP 向伺服器發送 EXAMINE/SELECT 更新最新信件！
                var inbox: Folder? = null
                try {
                    inbox = store.getFolder("INBOX")
                    inbox.open(Folder.READ_ONLY)

                    val totalCount = inbox.messageCount
                    if (totalCount > 0) {
                        val startMsg = (totalCount - 15).coerceAtLeast(1)
                        val messages = inbox.getMessages(startMsg, totalCount)

                        for (i in messages.indices.reversed()) {
                            val msg = messages[i]
                            val rawSubject = msg.subject ?: ""
                            val subject = try { MimeUtility.decodeText(rawSubject) } catch (_: Exception) { rawSubject }
                            val from = msg.from?.joinToString { it.toString() } ?: ""

                            val isSuggest = subject.contains("建言", ignoreCase = true) ||
                                    subject.contains("NKUST", ignoreCase = true) ||
                                    subject.contains("校務", ignoreCase = true) ||
                                    subject.contains("高雄科技大學", ignoreCase = true) ||
                                    from.contains("suggest", ignoreCase = true) ||
                                    from.contains("nkust", ignoreCase = true) ||
                                    from.contains("bboffice", ignoreCase = true)

                            val body = extractBodyText(msg)
                            val urls = findConfirmationUrls(body)

                            // 只要信件主旨符合建言特徵，或信件內文含有高科大校務建言系統確認連結，即判定成功找到確認信！
                            if (urls.isNotEmpty() && (isSuggest || urls.any { it.contains("suggest.nkust.edu.tw", ignoreCase = true) })) {
                                val targetUrl = urls.last()
                                onProgress("🎉 成功找到確認信！主旨: $subject")
                                Log.i("MailVerifier", "Found confirmation url: $targetUrl from subject: $subject")
                                return@withContext targetUrl
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("MailVerifier", "Error during inbox scan: ${e.message}")
                } finally {
                    try {
                        inbox?.close(false)
                    } catch (_: Exception) {}
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

    /**
     * 單次即時檢查收件匣中最近的一封建言確認信 (支援手動點擊「立即檢查」)
     */
    suspend fun checkLatestConfirmationUrl(onProgress: (String) -> Unit): String = withContext(Dispatchers.IO) {
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

        onProgress("正在連線郵件伺服器...")
        store.connect(host, port, username, password)

        try {
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val totalCount = inbox.messageCount
            if (totalCount == 0) {
                throw Exception("信箱內查無任何郵件")
            }

            val startMsg = (totalCount - 20).coerceAtLeast(1)
            val messages = inbox.getMessages(startMsg, totalCount)

            for (i in messages.indices.reversed()) {
                val msg = messages[i]
                val rawSubject = msg.subject ?: ""
                val subject = try { MimeUtility.decodeText(rawSubject) } catch (_: Exception) { rawSubject }
                val from = msg.from?.joinToString { it.toString() } ?: ""

                val isSuggest = subject.contains("建言", ignoreCase = true) ||
                        subject.contains("NKUST", ignoreCase = true) ||
                        subject.contains("校務", ignoreCase = true) ||
                        from.contains("suggest", ignoreCase = true) ||
                        from.contains("nkust", ignoreCase = true)

                val body = extractBodyText(msg)
                val urls = findConfirmationUrls(body)

                if (urls.isNotEmpty() && (isSuggest || urls.any { it.contains("suggest.nkust.edu.tw", ignoreCase = true) })) {
                    val targetUrl = urls.last()
                    onProgress("🎉 成功找到確認信！主旨: $subject")
                    return@withContext targetUrl
                }
            }

            throw Exception("最近 20 封信件中未找到任何校務建言確認連結")
        } finally {
            try {
                if (store.isConnected) store.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * 核心修復 3：深度解析 MimePart 本體，優先讀取 decoded inputStream 避開 Android Activation 缺失問題
     */
    private fun extractBodyText(part: Part): String {
        return try {
            if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                try {
                    part.inputStream.bufferedReader(Charsets.UTF_8).readText()
                } catch (_: Exception) {
                    part.content.toString()
                }
            } else if (part.isMimeType("multipart/*")) {
                val mp = try {
                    part.content as? Multipart
                } catch (_: Exception) {
                    try {
                        val ds = part.dataHandler?.dataSource
                            ?: javax.mail.util.ByteArrayDataSource(part.inputStream, part.contentType)
                        MimeMultipart(ds)
                    } catch (_: Exception) {
                        null
                    }
                } ?: return ""

                val sb = StringBuilder()
                for (i in 0 until mp.count) {
                    sb.append(extractBodyText(mp.getBodyPart(i))).append("\n")
                }
                sb.toString()
            } else {
                try {
                    part.inputStream.bufferedReader(Charsets.UTF_8).readText()
                } catch (_: Exception) {
                    ""
                }
            }
        } catch (e: Exception) {
            Log.w("MailVerifier", "extractBodyText exception: ${e.message}")
            ""
        }
    }

    /**
     * 核心修復 4：清除 Quoted-Printable 軟換行 (= 回車)、=3D 解碼、&amp; 解碼與大小寫無關比對
     */
    private fun findConfirmationUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // 消除 Quoted-Printable 殘留字元及軟換行
        val cleanedText = text
            .replace("=\r\n", "")
            .replace("=\n", "")
            .replace("=3D", "=")
            .replace("=3d", "=")

        val list = mutableListOf<String>()
        val regex = Pattern.compile("""https?://suggest\.nkust\.edu\.tw[^\s"'<>]+""", Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(cleanedText)

        while (matcher.find()) {
            var url = matcher.group() ?: continue
            // 消除結尾雜質標點與 HTML 符號
            url = url.trimEnd('.', ',', ';', '!', '。', '，', '；', '\\', '"', '\'', ')', ']', '>')
            url = url.replace("=3D", "=")
                .replace("=3d", "=")
                .replace("&amp;", "&")

            val lower = url.lowercase()
            if (lower.contains("confirm") ||
                lower.contains("message") ||
                lower.contains("verify") ||
                lower.contains("token") ||
                lower.contains("chk") ||
                lower.contains("key=")
            ) {
                list.add(url)
            }
        }
        return list
    }
}
