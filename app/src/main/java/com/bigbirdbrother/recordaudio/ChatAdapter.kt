package com.bigbirdbrother.recordaudio

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.stream.Stream


class ChatAdapter(private val context: Context, val messages: MutableList<Message>) :
    RecyclerView.Adapter<MessageViewHolder>() {
    // 添加标记状态集合
    var markedItems = mutableSetOf<Int>()

    // 添加选择状态集合（用于多选删除）
    private val selectedItems = mutableSetOf<Int>()

    // 检查是否标记
    fun isMarked(position: Int): Boolean = markedItems.contains(position)
    fun isSelected(position: Int): Boolean = selectedItems.contains(position)

    fun selectItem(position: Int) {
        selectedItems.add(position)
        notifyItemChanged(position)
    }

    fun deselectItem(position: Int) {
        selectedItems.remove(position)
        notifyItemChanged(position)
    }

    // 长按位置监听回调
    var onItemLongClick: ((Int) -> Unit)? = null


    // 长按回调接口
    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int, view: View)
    }

    var longClickListener: OnItemLongClickListener? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    longClickListener?.onItemLongClick(position, itemView)
                }
                false
            }
        }
    }

    // 处理菜单项点击
    fun handleContextMenuAction(itemId: Int, position: Int): Boolean {
        if (position !in 0 until messages.size) return false

        return when (itemId) {
            R.id.menu_mark -> {
                messages[position].isBookmarked = true
                notifyItemChanged(position)
                true
            }

            R.id.menu_unmark -> {
                messages[position].isBookmarked = false
                notifyItemChanged(position)
                true
            }

            else -> false
        }
    }


//    // 创建上下文菜单（在 Activity/Fragment 中调用）
//    fun createContextMenu(menu: ContextMenu, position: Int) {
//        val message = messages[position]
//        val inflater = context.menuInflater
//        inflater.inflate(R.menu.context_menu, menu)
//
//        // 动态切换菜单项可见性
//        menu.findItem(R.id.menu_mark).isVisible = !message.isBookmarked
//        menu.findItem(R.id.menu_unmark).isVisible = message.isBookmarked
//    }

    // 处理菜单点击事件（在 Activity/Fragment 中调用）
    fun handleMenuItemClick(itemId: Int, position: Int): Boolean {
        val message = messages[position]
        return when (itemId) {
            R.id.menu_mark -> {
                message.isBookmarked = true
                notifyItemChanged(position)
                true
            }

            R.id.menu_unmark -> {
                message.isBookmarked = false
                notifyItemChanged(position)
                true
            }

            else -> false
        }
    }

    fun getSelectedCount(): Int = selectedItems.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View
        if (viewType == USER_MESSAGE) {
            view = inflater.inflate(R.layout.item_user_message, parent, false)
        } else {
            view = inflater.inflate(R.layout.item_system_message, parent, false)
        }
        var holder = MessageViewHolder(view,viewType)
        // 注册上下文菜单
        (context as Activity).registerForContextMenu(holder.itemView)

        return holder
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.content

        // 设置标记状态
        holder.itemView.isActivated = markedItems.contains(position)

        // 设置选择状态
        holder.itemView.isSelected = selectedItems.contains(position)

        // 设置背景色
        when {
            selectedItems.contains(position) -> {
                holder.messageText.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.high_light
                    )
                )
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.purple_500
                    )
                )
                holder.messageText.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.black
                    )
                )


            }

             message.isBookmarked ->{
                 if (holder.viewType==USER_MESSAGE){
                     holder.messageText.setBackgroundColor(
                         ContextCompat.getColor(
                             holder.itemView.context,
                             R.color.purple_700
                         )
                     )
                     holder.messageText.setTextColor(
                         ContextCompat.getColor(
                             holder.itemView.context,
                             R.color.white
                         )
                     )
                 }else{
                     holder.messageText.setBackgroundColor(
                         ContextCompat.getColor(
                             holder.itemView.context,
                             R.color.green_blue
                         )
                     )
                     holder.messageText.setTextColor(
                         ContextCompat.getColor(
                             holder.itemView.context,
                             R.color.white
                         )
                     )
                 }
                 holder.itemView.setBackgroundColor(Color.TRANSPARENT)
             }


            else -> {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                if (holder.viewType==USER_MESSAGE){
                    holder.messageText.setBackgroundColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.user
                        )
                    )
                    holder.messageText.setTextColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.white
                        )
                    )
                }else{
                    holder.messageText.setBackgroundColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.system
                        )
                    )
                    holder.messageText.setTextColor(
                        ContextCompat.getColor(
                            holder.itemView.context,
                            R.color.black
                        )
                    )
                }

            }


        }

//        // 添加长按事件监听器
//        holder.messageText.setOnLongClickListener { v ->
//            showCustomMenu(v, message,position)
//            true
//        }
        // 设置长按监听器保存位置
        holder.itemView.setOnLongClickListener { v ->
            v.tag = holder.absoluteAdapterPosition // 保存当前位置到 tag
            // 确保视图已附加到窗口
            if (v.isAttachedToWindow) {
                // 添加轻微延迟确保菜单稳定显示
                v.postDelayed({
                    (context as Activity).openContextMenu(v)
                }, 50)
            }
            true // 返回 true 消费事件
        }
    }

    // 删除消息
    fun deleteMessageAndNotify(position: Int) {
        deleteMessageInAdapter(position)

        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)

        // 调整集合中大于删除位置的索引
        updateCollectionsAfterDeletion(position)
    }

    fun deleteMessageInAdapter(position: Int) {
        messages.removeAt(position)
        // 更新标记和选择集合
        markedItems.remove(position)
        selectedItems.remove(position)
    }

    // 删除消息
    fun deleteAllMessage() {
        val originalSize = messages.size
        if (originalSize > 0) {
            messages.clear()
            markedItems.clear()
            selectedItems.clear()
            this.notifyItemRangeRemoved(0, originalSize)
        }
    }

    // 切换标记状态
    fun toggleMark(position: Int) {
        if (markedItems.contains(position)) {
            markedItems.remove(position)
        } else {
            markedItems.add(position)
        }
        notifyItemChanged(position)
    }

    // 取消标记
    fun unMarkItem(position: Int) {
        if (markedItems.contains(position)) {
            markedItems.remove(position)
        }
        notifyItemChanged(position)
    }

    // 标记
    fun markItem(position: Int) {
        if (!markedItems.contains(position)) {
            markedItems.add(position)
        }
        notifyItemChanged(position)
    }

    // 启用多选模式
    fun enableMultiSelectMode(position: Int) {
        selectedItems.add(position)
        notifyItemChanged(position)

        // 通知 Activity 启动多选模式
        onMultiSelectModeListener?.onMultiSelectModeStarted()
    }

    // 清除选择
    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    // 删除选中的消息
    fun deleteSelectedItems() {
        // 按降序删除以避免位置变化问题
        val sortedPositions = selectedItems.sortedDescending()
        sortedPositions.forEach { position ->
            messages.removeAt(position)
            notifyItemRemoved(position)
        }

        // 清空选择集合并更新标记集合
        selectedItems.clear()
        updateMarkedItemsAfterDeletion(sortedPositions)

        // 刷新列表
        notifyItemRangeChanged(0, itemCount)
    }

    // 标记选中的消息
    fun markSelectedItems() {
        selectedItems.forEach { position ->
            markedItems.add(position)
            notifyItemChanged(position)
        }
        selectedItems.clear()
    }

    // 取消标记选中的消息
    fun unmarkSelectedItems() {
        selectedItems.forEach { position ->
            markedItems.remove(position)
            notifyItemChanged(position)
        }
        selectedItems.clear()
    }

    fun selectedStream(): Stream<Int> {
        return selectedItems.stream();
    }

    // 多选模式监听器
    interface OnMultiSelectModeListener {
        fun onMultiSelectModeStarted()
    }

    var onMultiSelectModeListener: OnMultiSelectModeListener? = null

    // 在 onBindViewHolder 中设置标记和多选状态

    // 辅助方法
    private fun updateCollectionsAfterDeletion(deletedPosition: Int) {
        // 更新标记集合
        val updatedMarked = markedItems.map { if (it > deletedPosition) it - 1 else it }.toSet()
        markedItems.clear()
        markedItems.addAll(updatedMarked)

        // 更新选择集合
        val updatedSelected = selectedItems.map { if (it > deletedPosition) it - 1 else it }.toSet()
        selectedItems.clear()
        selectedItems.addAll(updatedSelected)
    }

    // 删除后更新标记集合
    private fun updateMarkedItemsAfterDeletion(deletedPositions: List<Int>) {
        val updatedMarked = mutableSetOf<Int>()
        markedItems.forEach { position ->
            var shift = 0
            deletedPositions.forEach { deleted ->
                if (deleted <= position) shift++
            }
            updatedMarked.add(position - shift)
        }
        markedItems.clear()
        markedItems.addAll(updatedMarked)
    }

    private fun showCustomMenu(v: View, message: Message, position: Int) {
        val context = v.context
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


//        // 使用菜单资源文件
//        val popupMenu = PopupMenu(context, v)
//        val inflater = popupMenu.menuInflater
//        inflater.inflate(R.menu.context_menu, popupMenu.menu)
//
//
//        // 菜单项点击事件
//        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
//            val itemId = item.itemId
//            if (itemId == R.id.menu_copy) {
//                // 复制文本到剪贴板
//                val clip = ClipData.newPlainText("message", message.content)
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
//                return@setOnMenuItemClickListener true
//            } else if (itemId == R.id.menu_share) {
//                // 分享文本
//                val shareIntent =
//                    Intent(Intent.ACTION_SEND)
//                shareIntent.setType("text/plain")
//                shareIntent.putExtra(Intent.EXTRA_TEXT, message.content)
//                context.startActivity(Intent.createChooser(shareIntent, "分享消息"))
//                return@setOnMenuItemClickListener true
//            }else if (itemId == R.id.menu_select_copy) {
//                // 启动文本选择Activity
//                val intent = Intent(context, TextSelectionActivity::class.java)
//                intent.putExtra(TextSelectionActivity.EXTRA_TEXT, message.content);
//                // 传递消息位置，用于精确定位（可选）
//                intent.putExtra("position", position);
//                context.startActivity(intent);
//                return@setOnMenuItemClickListener true
//            }
//            false
//        }
//
//        popupMenu.show()
    }


    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) USER_MESSAGE else SYSTEM_MESSAGE
    }

    override fun getItemCount(): Int {
        return messages.size
    }

//    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        var messageText: TextView = itemView.findViewById<TextView>(R.id.messageText)
//    }

    companion object {
        private const val USER_MESSAGE = 0
        private const val SYSTEM_MESSAGE = 1
    }
}