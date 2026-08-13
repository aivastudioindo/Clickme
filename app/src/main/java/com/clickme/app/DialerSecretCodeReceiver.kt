package com.clickme.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Menerima secret code dari aplikasi telepon: *#7676#*#*
 * Dipakai sebagai cara membuka Clickme saat ikon launcher disembunyikan.
 */
class DialerSecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val data = intent?.data
        val code = data?.schemeSpecificPart
        Log.d("DialerSecret", "secret code: $code")
        if (code == "7676") {
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launch)
        }
    }
}
