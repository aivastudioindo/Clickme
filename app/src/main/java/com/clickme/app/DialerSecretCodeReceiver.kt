package com.clickme.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Membuka Clickme saat kode dial dimasukkan di aplikasi telepon.
 * Mendukung dua mekanisme agar bekerja di berbagai device/Android version:
 *  1. SECRET_CODE (*#7676#*#*) - standar, tapi sering diblokir OEM di Android 10+.
 *  2. NEW_OUTGOING_CALL - lebih reliable, kita intercept nomor yang di-dial dan
 *     batalkan panggilan (setResultData(null)) lalu buka app.
 */
class DialerSecretCodeReceiver : BroadcastReceiver() {
    companion object {
        private const val CODE = "7676"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        when (intent?.action) {
            "android.provider.Telephony.SECRET_CODE" -> {
                val code = intent.data?.schemeSpecificPart
                Log.d("DialerSecret", "secret code: $code")
                if (code == CODE) launch(context)
            }
            "android.intent.action.NEW_OUTGOING_CALL" -> {
                // Nomor yang akan di-dial ada di EXTRA_PHONE_NUMBER atau resultData.
                val dialed = intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
                    ?: intent.getStringExtra("EXTRA_PHONE_NUMBER")
                    ?: intent.getResultData()
                Log.d("DialerSecret", "outgoing call: $dialed")
                if (dialed != null && dialed.contains(CODE)) {
                    // Batalkan panggilan asli agar tidak benar-benar menelepon.
                    try { resultData = null } catch (_: Exception) {}
                    launch(context)
                }
            }
        }
    }

    private fun launch(context: Context) {
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launch)
    }
}
