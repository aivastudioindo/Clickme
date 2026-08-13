package com.clickme.app

import android.app.Application
import com.clickme.app.repo.NotificationRepository
import com.clickme.app.repo.NotificationStore

/**
 * Dipanggil paling awal saat proses app dibuat (sebelum Activity/Service).
 * Muat riwayat notifikasi dari disk agar tidak hilang saat app ditutup/di-kill.
 * Ini mengikuti pola repo Alfio010 (MyApplication) yang memuat data di startup.
 */
class ClickmeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val saved = NotificationStore.load(this)
        if (saved.isNotEmpty()) {
            NotificationRepository.seed(saved)
        }
    }
}
