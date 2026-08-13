package com.clickme.app.model

/**
 * Satu baris notifikasi yang ditangkap dari sistem.
 * Field dibuat lengkap agar isi pesan chat (WhatsApp dkk) muncul utuh,
 * bukan cuma title/text singkat.
 */
data class NotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val bigText: String,
    val conversationTitle: String,
    val groupKey: String?,
    val timestamp: Long,
    val lines: List<String> = emptyList(),
    var isNew: Boolean = true
) {
    /** True bila notifikasi ini adalah bagian dari grup (punya groupKey). */
    val isGroup: Boolean get() = !groupKey.isNullOrEmpty()
}
