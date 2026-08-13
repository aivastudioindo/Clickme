package com.clickme.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.clickme.app.databinding.FragmentSettingsBinding
import com.clickme.app.R

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGrant.setOnClickListener {
            (requireActivity() as? com.clickme.app.MainActivity)?.openListenerSettings()
        }

        val prefs = requireContext().getSharedPreferences("clickme_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("telegram_token", "") ?: ""
        val chatId = prefs.getString("telegram_chat_id", "") ?: ""
        val enabled = prefs.getBoolean("telegram_enabled", false)

        binding.editToken.setText(token)
        binding.editChatId.setText(chatId)
        binding.switchTelegram.isChecked = enabled
        updateStatus(enabled)

        binding.switchTelegram.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("telegram_enabled", isChecked).apply()
            updateStatus(isChecked)
        }

        binding.btnSaveTelegram.setOnClickListener {
            val newToken = binding.editToken.text.toString().trim()
            val newChatId = binding.editChatId.text.toString().trim()
            prefs.edit()
                .putString("telegram_token", newToken)
                .putString("telegram_chat_id", newChatId)
                .apply()
            android.widget.Toast.makeText(
                requireContext(),
                R.string.telegram_saved_ok,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        setupAppLock(prefs)
    }

    private fun setupAppLock(prefs: android.content.SharedPreferences) {
        val hasLock = prefs.getString("app_lock_hash", "")?.isNotEmpty() == true
        binding.switchLock.isChecked = hasLock
        updateLockStatus(hasLock)

        binding.switchLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Aktifkan: minta sandi lewat field, simpan hash
                val pwd = binding.editLockPassword.text.toString()
                if (pwd.length < 4) {
                    binding.switchLock.isChecked = false
                    android.widget.Toast.makeText(requireContext(), R.string.app_lock_too_short, android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                prefs.edit().putString("app_lock_hash", hash(pwd)).apply()
                updateLockStatus(true)
                android.widget.Toast.makeText(requireContext(), R.string.app_lock_saved_ok, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                // Nonaktifkan: hapus hash
                prefs.edit().remove("app_lock_hash").apply()
                updateLockStatus(false)
                android.widget.Toast.makeText(requireContext(), R.string.app_lock_removed_ok, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSetLock.setOnClickListener {
            val pwd = binding.editLockPassword.text.toString()
            if (pwd.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), R.string.app_lock_empty, android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pwd.length < 4) {
                android.widget.Toast.makeText(requireContext(), R.string.app_lock_too_short, android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("app_lock_hash", hash(pwd)).apply()
            binding.switchLock.isChecked = true
            updateLockStatus(true)
            android.widget.Toast.makeText(requireContext(), R.string.app_lock_saved_ok, android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.btnRemoveLock.setOnClickListener {
            prefs.edit().remove("app_lock_hash").apply()
            binding.switchLock.isChecked = false
            binding.editLockPassword.text?.clear()
            updateLockStatus(false)
            android.widget.Toast.makeText(requireContext(), R.string.app_lock_removed_ok, android.widget.Toast.LENGTH_SHORT).show()
        }

        setupHideApp(prefs)
    }

    private fun setupHideApp(prefs: android.content.SharedPreferences) {
        val pm = requireContext().packageManager
        val component = android.content.ComponentName(requireContext(), MainActivity::class.java)
        val hidden = pm.getComponentEnabledSetting(component) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        binding.btnHideApp.setOnClickListener {
            if (hidden) {
                // Tampilkan kembali ikon launcher
                pm.setComponentEnabledSetting(
                    component,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                binding.hideStatus.text = getString(R.string.unhide_app_done)
                android.widget.Toast.makeText(requireContext(), R.string.unhide_app_done, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                // Sembunyikan ikon launcher (app tetap bisa dibuka lewat *#7676#*#*)
                prefs.edit().putBoolean("app_hidden", true).apply()
                pm.setComponentEnabledSetting(
                    component,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                binding.hideStatus.text = getString(R.string.hide_app_done)
                android.widget.Toast.makeText(requireContext(), R.string.hide_app_done, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLockStatus(enabled: Boolean) {
        binding.lockStatus.text =
            if (enabled) getString(R.string.app_lock_status_on)
            else getString(R.string.app_lock_status_off)
    }

    private fun hash(input: String): String {
        return try {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input
        }
    }

    private fun updateStatus(enabled: Boolean) {
        binding.telegramStatus.text =
            if (enabled) getString(R.string.telegram_status_on)
            else getString(R.string.telegram_status_off)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
