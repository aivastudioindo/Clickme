package com.clickme.app

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Mengirim notifikasi ke Telegram bot via Bot API (sendMessage).
 * Tidak mengubah alur capture/notifikasi yang sudah berjalan — hanya
 * dipanggil sebagai side-effect setelah notifikasi disimpan.
 */
object TelegramSender {
    private const val TAG = "TelegramSender"

    fun send(botToken: String, chatId: String, title: String, text: String) {
        if (botToken.isBlank() || chatId.isBlank()) return
        val safeTitle = title.ifBlank { "(tanpa judul)" }
        val safeText = text.ifBlank { "(tanpa isi)" }
        val message = buildString {
            append("📱 *Notifikasi*\n")
            append("**$safeTitle**\n")
            append(safeText)
        }
        val encoded = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (_: Exception) {
            URLEncoder.encode(safeText, "UTF-8")
        }
        val urlStr =
            "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&parse_mode=Markdown&text=$encoded"
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doInput = true
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "Telegram send failed: HTTP $code")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Telegram send error: ${e.message}")
        }
    }
}
