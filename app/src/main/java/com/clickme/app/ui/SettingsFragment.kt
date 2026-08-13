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

        binding.editToken.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefs.edit().putString("telegram_token", binding.editToken.text.toString().trim()).apply()
            }
        }
        binding.editChatId.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefs.edit().putString("telegram_chat_id", binding.editChatId.text.toString().trim()).apply()
            }
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
