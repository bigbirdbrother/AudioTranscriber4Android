package com.bigbirdbrother.recordaudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        holder.messageText.setText(messages[position].content)
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