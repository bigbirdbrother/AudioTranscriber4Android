package com.bigbirdbrother.recordaudio

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.core.content.edit

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("server_url")?.setOnPreferenceClickListener {
            showUrlSettingDialog()
            true
        }

        // 初始化摘要
        updateSummary()
    }

    private fun showUrlSettingDialog() {
        val sharedPrefs = preferenceManager.sharedPreferences
        val currentUrl = sharedPrefs?.getString("server_url", "") ?: ""

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_url_setting, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.et_server_url)
        etUrl.setText(currentUrl)

        AlertDialog.Builder(requireContext())
            .setTitle("配置语音识别服务地址")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
//                val newUrl = etUrl.text.toString().trim()
//                sharedPrefs?.edit { putString("server_url", newUrl) }
//                updateSummary(newUrl)
                updateSummary()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateSummary(url: String) {
        findPreference<Preference>("server_url")?.summary =
            if (url.isNotEmpty()) url else "未配置（使用默认地址）"
    }

    override fun onResume() {
        super.onResume()
        updateSummary()
    }

    private fun updateSummary() {
        val url = preferenceManager.sharedPreferences?.getString("server_url", "") ?: ""
        findPreference<Preference>("server_url")?.summary =
            if (url.isNotEmpty()) url else "未配置（使用默认地址）"
    }
}