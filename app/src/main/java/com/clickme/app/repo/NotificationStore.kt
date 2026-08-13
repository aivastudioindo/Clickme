package com.clickme.app.repo

import android.content.Context
import com.clickme.app.model.NotificationItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan riwayat notifikasi ke file lokal (JSON) agar tidak hilang
 * saat aplikasi ditutup atau di-kill. Repository di memori akan diisi ulang
 * dari file ini saat app dibuka kembali.
 */
object NotificationStore {
    private const val FILE_NAME = "notifications.json"
    private const val MAX_ITEMS = 500

    fun save(context: Context, items: List<NotificationItem>) {
        try {
            val arr = JSONArray()
            items.take(MAX_ITEMS).forEach { it ->
                val o = JSONObject()
                o.put("id", it.id)
                o.put("packageName", it.packageName)
                o.put("appName", it.appName)
                o.put("title", it.title)
                o.put("text", it.text)
                o.put("bigText", it.bigText)
                o.put("conversationTitle", it.conversationTitle)
                o.put("groupKey", it.groupKey ?: "")
                o.put("timestamp", it.timestamp)
                val linesArr = org.json.JSONArray()
                it.lines.forEach { line -> linesArr.put(line) }
                o.put("lines", linesArr)
                arr.put(o)
            }
            context.applicationContext.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(arr.toString().toByteArray())
            }
        } catch (_: Exception) {
            // gagal simpan tidak boleh menghentikan service
        }
    }

    fun load(context: Context): List<NotificationItem> {
        return try {
            val file = context.applicationContext.getFileStreamPath(FILE_NAME)
            if (!file.exists()) return emptyList()
            val txt = file.readText()
            if (txt.isBlank()) return emptyList()
            val arr = JSONArray(txt)
            val list = mutableListOf<NotificationItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    NotificationItem(
                        id = o.optString("id"),
                        packageName = o.optString("packageName"),
                        appName = o.optString("appName"),
                        title = o.optString("title"),
                        text = o.optString("text"),
                        bigText = o.optString("bigText"),
                        conversationTitle = o.optString("conversationTitle"),
                        groupKey = o.optString("groupKey").ifEmpty { null },
                        timestamp = o.optLong("timestamp"),
                        lines = run {
                            val arr = o.optJSONArray("lines")
                            val list = mutableListOf<String>()
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    list.add(arr.optString(j))
                                }
                            }
                            list
                        }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
