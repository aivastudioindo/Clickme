package com.clickme.app

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.clickme.app.databinding.ActivityMainBinding
import com.clickme.app.ui.NotificationsFragment
import com.clickme.app.ui.SettingsFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topbar)

        drawer = binding.drawer
        val toggle = ActionBarDrawerToggle(
            this, drawer, binding.topbar,
            R.string.drawer_open, R.string.drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        binding.drawer.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, NotificationsFragment())
                .commit()
            binding.navView.setCheckedItem(R.id.nav_notifications)
        }

        // Arahkan user ke pengaturan izin notifikasi kalau belum aktif
        if (!isListenerEnabled()) {
            Toast.makeText(this, R.string.grant_permission_hint, Toast.LENGTH_LONG).show()
        }
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_notifications -> replaceFragment(NotificationsFragment())
            R.id.nav_settings -> replaceFragment(SettingsFragment())
            R.id.nav_search -> Toast.makeText(this, R.string.soon_search, Toast.LENGTH_SHORT).show()
            R.id.nav_export -> Toast.makeText(this, R.string.soon_export, Toast.LENGTH_SHORT).show()
            R.id.nav_about -> Toast.makeText(this, R.string.about_text, Toast.LENGTH_SHORT).show()
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    private fun isListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotificationService::class.java)
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").map { it.substringBefore("/") }.contains(cn.flattenToString())
    }

    fun openListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: Exception) {
        }
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
