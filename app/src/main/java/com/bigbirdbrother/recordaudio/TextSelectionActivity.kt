package com.bigbirdbrother.recordaudio

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.View.OnLongClickListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min


class TextSelectionActivity : AppCompatActivity() {
    private var fullscreenTextView: TextView? = null
    private var selectionStart = -1
    private var selectionEnd = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_selection)

        fullscreenTextView = findViewById<TextView>(R.id.fullscreen_text_view)
        val text = intent.getStringExtra(EXTRA_TEXT)
        fullscreenTextView?.setText(text)
        fullscreenTextView?.setTextSize(24f) // 放大字体

        // 设置长按选择监听
        fullscreenTextView?.setOnLongClickListener(OnLongClickListener { v: View? ->
            // 清除之前的选中状态
            clearSelection()
            false // 让系统处理默认的选择行为
        })

        // 添加文本选择监听
        fullscreenTextView?.setOnClickListener(View.OnClickListener { v: View? ->
            if (selectionStart >= 0 && selectionEnd >= 0) {
                // 复制选中的文本
                copySelectedText()
                finish()
            }
        })

        // 添加选择变化监听
        fullscreenTextView?.addOnLayoutChangeListener(OnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
            if (selectionStart < 0 || selectionEnd < 0) {
                // 默认全选
                fullscreenTextView?.setSelectAllOnFocus(true)
                fullscreenTextView?.requestFocus()
            }
        })
    }

    private fun clearSelection() {
        val text = fullscreenTextView!!.text
        if (text is Spannable) {
            val spannable = text
            val spans = spannable.getSpans(
                0, text.length,
                BackgroundColorSpan::class.java
            )
            for (span in spans) {
                spannable.removeSpan(span)
            }
        }
        selectionStart = -1
        selectionEnd = -1
    }

    private fun updateSelection() {
        if (selectionStart < 0 || selectionEnd < 0) return

        val text = fullscreenTextView!!.text
        if (text is Spannable) {
            // 清除之前的选中状态
            clearSelection()


            // 设置新的选中状态
            val start = min(selectionStart.toDouble(), selectionEnd.toDouble()).toInt()
            val end = max(selectionStart.toDouble(), selectionEnd.toDouble()).toInt()

            text.setSpan(
                BackgroundColorSpan(ContextCompat.getColor(this, R.color.selection_highlight)),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun copySelectedText() {
        if (selectionStart >= 0 && selectionEnd >= 0) {
            val start = min(selectionStart.toDouble(), selectionEnd.toDouble()).toInt()
            val end = max(selectionStart.toDouble(), selectionEnd.toDouble()).toInt()
            val selectedText = fullscreenTextView!!.text.subSequence(start, end).toString()

            val clipboard =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText("selected_text", selectedText)
            clipboard.setPrimaryClip(clip)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (selectionStart >= 0 && selectionEnd >= 0) {
                clearSelection()
                return true
            }
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // 公开方法用于设置选择范围（从适配器调用）
    fun setSelection(start: Int, end: Int) {
        selectionStart = start
        selectionEnd = end
        updateSelection()
    }

    companion object {
        const val EXTRA_TEXT: String = "extra_text"
    }
}