package com.clickme.app

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.collection.LruCache
import com.clickme.app.BuildConfig
import com.clickme.app.model.NotificationItem
import com.clickme.app.repo.NotificationRepository
import java.util.concurrent.Executors

/**
 * Service yang menangkap semua notifikasi sistem.
 *
 * Anti-terlewat:
 *  - onListenerDisconnected -> requestRebind
 *  - onStartCommand -> requestRebind + START_STICKY
 * Anti-crash:
 *  - seluruh ekstraksi dibungkus try/catch (OS akan disable listener kalau crash)
 *  - null-safe sbn?.notification?.extras
 * Anti-tidak-akurat:
 *  - ekstraksi berurutan dengan fallback (title/text/bigText/textLines)
 */
class NotificationService : NotificationListenerService() {

    private val executor = Executors.newSingleThreadExecutor()
    private val recentNotificationsCache = LruCache<String, LastNotiData>(30)
    // Dedupe khusus pengiriman Telegram agar notif sama tidak dikirim berulang-ulang.
    private val telegramSentCache = LruCache<String, Long>(100)

    private data class LastNotiData(val title: String, val text: String, val date: Long)

    companion object {
        private const val DUPLICATE_WINDOW_MS = 30_000L
        private val SYSTEM_BLACKLIST = setOf(
            "android",
            "com.android.systemui",
            "com.android.providers.downloads",
            "com.android.providers.media",
            "com.android.packageinstaller"
        )

        /**
         * Catch-up: ambil notifikasi yang sedang aktif di sistem dan masukkan ke repository.
         * Dipanggil saat user tarik-refresh, agar list tidak kosong meski app baru dibuka.
         */
        fun requestActiveNotifications(context: Context) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val svc = context as? NotificationListenerService
                for (sbn in nm.activeNotifications) {
                    // Lewat capture() agar dedupe & penyimpanan konsisten dengan
                    // notifikasi yang masuk secara live.
                    svc?.let { (it as? com.clickme.app.NotificationService)?.capturePublic(sbn) }
                        ?: run {
                            val item = buildItem(sbn, context.packageManager) ?: return@run
                            NotificationRepository.add(item)
                        }
                }
            } catch (_: Exception) {
            }
        }

        private fun buildItem(sbn: StatusBarNotification, pm: PackageManager): NotificationItem? {
            val pkg = sbn.packageName ?: return null
            if (pkg in SYSTEM_BLACKLIST) return null
            val extras: Bundle = sbn.notification?.extras ?: return null
            val data = extract(extras)
            if (data.title.isEmpty() && data.text.isEmpty() && data.lines.isEmpty()) return null
            val appName = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                pkg
            }
            // Untuk notifikasi bertumpuk, tampilkan baris pertama sebagai ringkasan
            // dan simpan semua baris ke `lines` agar bisa di-expand di UI.
            val summaryText = if (data.lines.isNotEmpty()) {
                data.lines.first()
            } else {
                data.text.ifEmpty { data.bigText }
            }
            return NotificationItem(
                id = "${pkg}_${sbn.key}_${sbn.notification.`when`}",
                packageName = pkg,
                appName = appName,
                title = data.title.ifEmpty { data.conversationTitle },
                text = summaryText,
                bigText = data.bigText,
                conversationTitle = data.conversationTitle,
                groupKey = sbn.groupKey,
                timestamp = sbn.notification.`when`.takeIf { it > 0 } ?: System.currentTimeMillis(),
                lines = data.lines
            )
        }

        private fun extract(extras: Bundle): NotiData {
            val title = orEmpty(extras.getCharSequence(Notification.EXTRA_TITLE))
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_TITLE_BIG)) }
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)) }

            // Prioritas teks: EXTRA_TEXT -> EXTRA_BIG_TEXT -> EXTRA_TEXT_LINES.
            // EXTRA_INFO_TEXT tidak dipakai sebagai fallback utama karena sering kosong
            // (misal pada email/WhatsApp isi pesan ada di BIG_TEXT atau LINES).
            val text = orEmpty(extras.getCharSequence(Notification.EXTRA_TEXT))
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)) }

            val bigText = orEmpty(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
            val conversationTitle = orEmpty(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))

            // Notifikasi bertumpuk (misal WhatsApp grup) menyimpan tiap pesan di EXTRA_TEXT_LINES.
            val lines = fromLines(extras)

            return NotiData(title, text, bigText, conversationTitle, lines)
        }

        private fun fromLines(extras: Bundle): List<String> {
            val seqs: List<CharSequence>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    extras.getCharSequenceArrayList(Notification.EXTRA_TEXT_LINES)
                        as? ArrayList<CharSequence>
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            } ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.toList()

            return seqs?.filterNotNull()
                ?.map { it.toString().trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        }

        private fun orEmpty(cs: CharSequence?): String = cs?.toString()?.trim() ?: ""

        private data class NotiData(
            val title: String,
            val text: String,
            val bigText: String,
            val conversationTitle: String,
            val lines: List<String>
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val safe = sbn ?: return
        // Ikuti pendekatan repo Alfio010: terima semua notifikasi (termasuk summary
        // grup), lalu dedupe + blacklist spesifik. Tidak membuang notif agar tidak
        // ada pesan yang terlewat.
        if (shouldIgnore(safe)) return
        executor.execute {
            try {
                capture(safe)
            } catch (_: Exception) {
                // jangan biarkan crash mematikan service
            }
        }
    }

    private fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        // Mengikuti logika repo referensi Alfio010 (NotificationListenerServiceImpl.shouldIgnoreNotification
        // + NotiUtils.isAutoBlacklistedNotification + shouldDropByDefaultBlacklist).
        return isAutoBlacklisted(sbn) || shouldDropByDefaultBlacklist(sbn)
    }

    // Setara NotiUtils.isAutoBlacklistedNotification (tanpa gate getAutoBlacklistOn,
    // karena Clickme tidak punya setting itu -> dianggap selalu aktif seperti referensi).
    private fun isAutoBlacklisted(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName ?: return true
        if (pkg.startsWith("com.whatsapp") && (sbn.key?.contains("null") == true)) return true
        if (pkg == "com.sec.android.app.clock.package") return true
        if (pkg == BuildConfig.APPLICATION_ID) return true
        if (sbn.key == "-1|android|27|null|1000") return true
        if (sbn.key == "charging_state") return true
        if (sbn.key == "com.sec.android.app.samsungapps|121314|null|10091") return true
        return false
    }

    // Setara NotiUtils.shouldDropByDefaultBlacklist: buang notif ongoing & kategori sistem.
    private fun shouldDropByDefaultBlacklist(sbn: StatusBarNotification): Boolean {
        if (sbn.isOngoing) return true
        if (sbn.notification.category == Notification.CATEGORY_SYSTEM) return true
        return false
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // notifikasi dihapus tidak kita hapus dari list agar riwayat tetap ada
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        try {
            requestRebind(ComponentName(this, this::class.java))
        } catch (_: Exception) {
        }
        return START_STICKY
    }

    /** Dipanggil dari companion requestActiveNotifications untuk catch-up. */
    fun capturePublic(sbn: StatusBarNotification) {
        if (shouldIgnore(sbn)) return
        capture(sbn)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Listener siap. Tarik notifikasi yang masih aktif di sistem agar riwayat
        // tidak kosong saat app dibuka kembali (capture hanya dipanggil untuk notif baru).
        requestActiveNotifications(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            requestRebind(ComponentName(this, this::class.java))
        } catch (_: Exception) {
        }
    }

    private fun capture(sbn: StatusBarNotification) {
        val extras: Bundle = sbn.notification?.extras ?: return
        val data = extract(extras)

        // Sama seperti repo Alfio010: dedupe per package + window 30 detik.
        if (data.title.isEmpty() && data.text.isEmpty()) return

        val current = LastNotiData(data.title, data.text, sbn.notification.`when`)
        val last = recentNotificationsCache[sbn.packageName]
        if (last == current) return
        if (last != null &&
            last.title == current.title &&
            last.text == current.text &&
            current.date - last.date <= DUPLICATE_WINDOW_MS
        ) {
            return
        }
        recentNotificationsCache.put(sbn.packageName, current)

        val item = buildItem(sbn, packageManager) ?: return
        NotificationRepository.add(item)
        NotificationRepository.saveToDisk(applicationContext)

        // Forward ke Telegram (jika diaktifkan di pengaturan). Tidak mengubah
        // alur penyimpanan notifikasi yang sudah berjalan.
        forwardToTelegram(item)
    }

    private fun forwardToTelegram(item: com.clickme.app.model.NotificationItem) {
        try {
            val prefs = getSharedPreferences("clickme_prefs", android.content.Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("telegram_enabled", false)
            if (!enabled) return
            val token = prefs.getString("telegram_token", "") ?: ""
            val chatId = prefs.getString("telegram_chat_id", "") ?: ""
            if (token.isBlank() || chatId.isBlank()) return
            val title = item.title.ifEmpty { item.conversationTitle }
            val text = item.text.ifEmpty { item.bigText }.ifEmpty { item.lines.firstOrNull() ?: "" }

            // Dedupe: jangan kirim notif dengan isi sama dalam 30 detik terakhir.
            // Mencegah pesan berulang di Telegram saat app sumber mem-post ulang
            // notifikasi (update) atau saat catch-up memindai ulang.
            val key = "${item.packageName}|${title}|${text}"
            val now = System.currentTimeMillis()
            val lastSent = telegramSentCache[key] ?: 0L
            if (now - lastSent < DUPLICATE_WINDOW_MS) return
            telegramSentCache.put(key, now)

            executor.execute {
                TelegramSender.send(token, chatId, item.appName, title, text)
            }
        } catch (_: Exception) {
            // jangan biarkan error Telegram mengganggu capture notifikasi
        }
    }
}
