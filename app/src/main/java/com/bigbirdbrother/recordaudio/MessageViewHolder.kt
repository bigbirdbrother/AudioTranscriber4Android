package com.bigbirdbrother.recordaudio
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageViewHolder(itemView: View, viewType_input: Int) : RecyclerView.ViewHolder(itemView) {
    var viewType: Int = viewType_input
    var messageText: TextView = itemView.findViewById<TextView>(R.id.messageText)
    init {
        itemView.setOnClickListener {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
//                var mainActivity = (itemView.context) as MainActivity
//                if (mainActivity.actionMode!=null){
//                    if (isSelected(position) == true) {
//                        adapter?.deselectItem(position)
//                        if (adapter?.getSelectedCount() == 0) {
//                            actionMode?.finish()
//                        }
//                    } else {
//                        adapter?.selectItem(position)
//                    }
//                }
                (itemView.context as MainActivity).onMessageItemClick(position)
            }
        }
        messageText.setOnClickListener {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                (itemView.context as MainActivity).onMessageItemClick(position)
            }
        }

    }
}