package com.bigbirdbrother.recordaudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView


class ChatAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View
        if (viewType == USER_MESSAGE) {
            view = inflater.inflate(R.layout.item_user_message, parent, false)
        } else {
            view = inflater.inflate(R.layout.item_system_message, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.content


        // 添加长按事件监听器
        holder.messageText.setOnLongClickListener { v ->
            showCustomMenu(v, message,position)
            true
        }
    }

    private fun showCustomMenu(v: View, message: Message, position: Int) {
        val context = v.context
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


        // 使用菜单资源文件
        val popupMenu = PopupMenu(context, v)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.context_menu, popupMenu.menu)


        // 菜单项点击事件
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            val itemId = item.itemId
            if (itemId == R.id.menu_copy) {
                // 复制文本到剪贴板
                val clip = ClipData.newPlainText("message", message.content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                return@setOnMenuItemClickListener true
            } else if (itemId == R.id.menu_share) {
                // 分享文本
                val shareIntent =
                    Intent(Intent.ACTION_SEND)
                shareIntent.setType("text/plain")
                shareIntent.putExtra(Intent.EXTRA_TEXT, message.content)
                context.startActivity(Intent.createChooser(shareIntent, "分享消息"))
                return@setOnMenuItemClickListener true
            }else if (itemId == R.id.menu_select_copy) {
                // 启动文本选择Activity
                val intent = Intent(context, TextSelectionActivity::class.java)
                intent.putExtra(TextSelectionActivity.EXTRA_TEXT, message.content);
                // 传递消息位置，用于精确定位（可选）
                intent.putExtra("position", position);
                context.startActivity(intent);
                return@setOnMenuItemClickListener true
            }
            false
        }

        popupMenu.show()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) USER_MESSAGE else SYSTEM_MESSAGE
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var messageText: TextView = itemView.findViewById<TextView>(R.id.messageText)
    }

    companion object {
        private const val USER_MESSAGE = 0
        private const val SYSTEM_MESSAGE = 1
    }
}