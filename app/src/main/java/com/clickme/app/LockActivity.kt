package com.clickme.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        val prefs = getSharedPreferences("clickme_prefs", Context.MODE_PRIVATE)
        val storedHash = prefs.getString("app_lock_hash", "") ?: ""

        val input = findViewById<EditText>(R.id.lockInput)
        val btn = findViewById<Button>(R.id.btnUnlock)
        val wrong = findViewById<TextView>(R.id.lockWrong)

        btn.setOnClickListener {
            val pwd = input.text.toString()
            if (pwd.isEmpty()) {
                Toast.makeText(this, R.string.app_lock_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val enteredHash = hash(pwd)
            if (enteredHash == storedHash) {
                // Buka: set flag session agar tidak diminta lagi di sesi ini
                getSharedPreferences("clickme_session", Context.MODE_PRIVATE)
                    .edit().putBoolean("unlocked", true).apply()
                finish()
            } else {
                wrong.visibility = android.view.View.VISIBLE
                input.text.clear()
            }
        }
    }

    private fun hash(input: String): String {
        return try {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input
        }
    }
}
