package com.clickme.app.repo

import androidx.collection.LruCache
import com.clickme.app.model.NotificationItem

/**
 * Penyimpanan notifikasi in-memory dengan observer pattern agar UI refresh live.
 * Deduplikasi berbasis id unik (pkg_key_when) agar notifikasi yang sama tidak
 * muncul berulang, baik dari live capture maupun catch-up requestActiveNotifications.
 */
object NotificationRepository {
    private val items = mutableListOf<NotificationItem>()
    private val listeners = mutableListOf<(List<NotificationItem>) -> Unit>()
    private val seenIds = LinkedHashSet<String>()

    fun add(item: NotificationItem) {
        if (item.id in seenIds) return
        seenIds.add(item.id)
        items.add(0, item)
        listeners.forEach { it(getAll()) }
    }

    /** Isi repository dari penyimpanan disk (tanpa memicu notifikasi ganda). */
    fun seed(itemsToAdd: List<NotificationItem>) {
        for (it in itemsToAdd) {
            if (it.id !in seenIds) {
                seenIds.add(it.id)
                items.add(0, it)
            }
        }
        listeners.forEach { it(getAll()) }
    }

    fun saveToDisk(context: android.content.Context) {
        NotificationStore.save(context, getAll())
    }

    fun getAll(): List<NotificationItem> = items.toList()

    fun observe(callback: (List<NotificationItem>) -> Unit) {
        listeners.add(callback)
        callback(getAll())
    }

    fun removeObserver(callback: (List<NotificationItem>) -> Unit) {
        listeners.remove(callback)
    }

    fun markAllRead() {
        items.forEach { it.isNew = false }
        listeners.forEach { it(getAll()) }
    }

    fun clear() {
        items.clear()
        seenIds.clear()
        listeners.forEach { it(getAll()) }
    }
}
