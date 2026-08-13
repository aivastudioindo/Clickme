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
                val pm = context.packageManager
                for (sbn in nm.activeNotifications) {
                    val item = buildItem(sbn, pm) ?: continue
                    NotificationRepository.add(item)
                }
            } catch (_: Exception) {
            }
        }

        private fun buildItem(sbn: StatusBarNotification, pm: PackageManager): NotificationItem? {
            val pkg = sbn.packageName ?: return null
            if (pkg in SYSTEM_BLACKLIST) return null
            val extras: Bundle = sbn.notification?.extras ?: return null
            val data = extract(extras)
            if (data.title.isEmpty() && data.text.isEmpty()) return null
            val appName = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                pkg
            }
            return NotificationItem(
                id = "${pkg}_${sbn.key}_${sbn.notification.`when`}",
                packageName = pkg,
                appName = appName,
                title = data.title,
                text = data.text,
                bigText = data.bigText,
                conversationTitle = data.conversationTitle,
                groupKey = sbn.groupKey,
                timestamp = sbn.notification.`when`.takeIf { it > 0 } ?: System.currentTimeMillis()
            )
        }

        private fun extract(extras: Bundle): NotiData {
            val title = orEmpty(extras.getCharSequence(Notification.EXTRA_TITLE))
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_TITLE_BIG)) }
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)) }

            val text = orEmpty(extras.getCharSequence(Notification.EXTRA_TEXT))
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)) }
                .ifEmpty { fromLines(extras) }
                .ifEmpty { orEmpty(extras.getCharSequence(Notification.EXTRA_INFO_TEXT)) }

            val bigText = orEmpty(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
            val conversationTitle = orEmpty(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))
            return NotiData(title, text, bigText, conversationTitle)
        }

        private fun fromLines(extras: Bundle): String {
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return ""
            return lines.filterNotNull().joinToString("\n") { it.toString() }.trim()
        }

        private fun orEmpty(cs: CharSequence?): String = cs?.toString()?.trim() ?: ""

        private data class NotiData(
            val title: String,
            val text: String,
            val bigText: String,
            val conversationTitle: String
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val safe = sbn ?: return
        executor.execute {
            try {
                capture(safe)
            } catch (_: Exception) {
                // jangan biarkan crash mematikan service
            }
        }
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
        val pkg = sbn.packageName ?: return
        if (pkg in SYSTEM_BLACKLIST) return

        val extras: Bundle = sbn.notification?.extras ?: return
        val data = extract(extras)

        if (data.title.isEmpty() && data.text.isEmpty()) return

        val current = LastNotiData(data.title, data.text, sbn.notification.`when`)
        val last = recentNotificationsCache[pkg]
        if (last == current) return
        if (last != null &&
            last.title == current.title &&
            last.text == current.text &&
            current.date - last.date <= DUPLICATE_WINDOW_MS
        ) {
            return
        }
        recentNotificationsCache.put(pkg, current)

        val item = buildItem(sbn, packageManager) ?: return
        NotificationRepository.add(item)
    }
}
