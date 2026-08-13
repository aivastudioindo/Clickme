package com.clickme.app.repo

import androidx.collection.LruCache
import com.clickme.app.model.NotificationItem

/**
 * Penyimpanan notifikasi in-memory dengan observer pattern agar UI refresh live,
 * plus deduplikasi sederhana per packageName (window singkat) agar tidak spam duplikat.
 */
object NotificationRepository {
    private val items = mutableListOf<NotificationItem>()
    private val listeners = mutableListOf<(List<NotificationItem>) -> Unit>()
    private val recentCache = LruCache<String, Pair<String, String>>(30)

    fun add(item: NotificationItem) {
        val last = recentCache[item.packageName]
        if (last?.first == item.title && last.second == item.text) return
        recentCache.put(item.packageName, item.title to item.text)
        items.add(0, item)
        listeners.forEach { it(getAll()) }
    }

    /** Isi repository dari penyimpanan disk (tanpa memicu notifikasi ganda). */
    fun seed(itemsToAdd: List<NotificationItem>) {
        items.addAll(0, itemsToAdd)
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
        recentCache.evictAll()
        listeners.forEach { it(getAll()) }
    }
}
