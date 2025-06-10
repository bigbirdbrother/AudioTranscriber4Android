package com.bigbirdbrother.recordaudio

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 沉浸式状态栏设置
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 设置状态栏透明
        window.statusBarColor = Color.TRANSPARENT

        // 设置返回按钮
        val toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "设置"

        // 设置工具栏顶部内边距（适配状态栏）
        toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
        toolbar.layoutParams.height += getStatusBarHeight()

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    // 获取状态栏高度
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}