package com.bigbirdbrother.recordaudio

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.ContextMenu
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room.databaseBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer


class MainActivity : AppCompatActivity(), ChatAdapter.OnMultiSelectModeListener {
    private var btn_record: Button? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: ChatAdapter? = null
    private val messages: MutableList<Message> = ArrayList<Message>()
    private var audioRecorder: AudioRecorder? = null
    private var outputDir: File? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var db: AppDatabase? = null

    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var toolbar: MaterialToolbar? = null
    private var statusBarBackground: View? = null

    private var isFileUploadMode = false

    //    private lateinit var outputDir: File
    private val FILE_PICK_REQUEST_CODE = 1001

    public var actionMode: ActionMode? = null

    private var longClickedPosition = -1
    private var selectedPosition: Int = -1 // 存储当前选中的位置
    private var lastMenuView: View? = null
    private var isContextMenuShowing = false // 添加菜单状态跟踪

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // 确保上下文是 Activity
            if (this !is Activity) {
                Log.e("Permissions", "不是 Activity 实例，不能请求权限")
                return
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permissions = mutableListOf<String>()

                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                }

                Log.d("Permissions", "请求权限: ${permissions.joinToString()}")
                if (permissions.isNotEmpty()) {
                    ActivityCompat.requestPermissions(
                        this,
                        permissions.toTypedArray(),
                        REQUEST_PERMISSIONS
                    )
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
            // 这里可以弹个提示，或者做其他逻辑处理
        }


        // 设置状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        // 初始化视图
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        toolbar = findViewById(R.id.toolbar)
        statusBarBackground = findViewById(R.id.status_bar_background);

        // 设置状态栏占位高度
//        setStatusBarHeight();

        // 设置工具栏
//        setSupportActionBar(toolbar)
        toolbar?.setNavigationOnClickListener { v -> drawerLayout?.openDrawer(GravityCompat.END) }


        // 设置菜单项点击事件
        navigationView?.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    // 打开设置页面
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout?.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.menu_clear_history -> {  // 新增处理逻辑
                    // 清空历史数据的逻辑
                    clearHistoryData()
                    drawerLayout?.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.menu_tutorial -> {  // 新增教程菜单处理
                    startActivity(Intent(this, TutorialActivity::class.java))
                    drawerLayout?.closeDrawer(GravityCompat.END)
                    true
                }
                else -> false
            }
        }


        // 初始化数据库
        db = databaseBuilder(
            applicationContext, AppDatabase::class.java, "chat-database"
        ).fallbackToDestructiveMigration() // 添加这行
            .build()


        // 初始化UI
        btn_record = findViewById<Button>(R.id.btn_record)
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val parentLayout = findViewById<ConstraintLayout>(R.id.layout_main)
        unregisterForContextMenu(parentLayout)  // 保险操作

        adapter = ChatAdapter(this, messages)
        adapter?.onMultiSelectModeListener = this
        recyclerView?.setLayoutManager(LinearLayoutManager(this))
        recyclerView?.adapter = adapter
        // 为 RecyclerView 注册上下文菜单
        registerForContextMenu(recyclerView)
//        // 设置长按位置监听
//        adapter?.onItemLongClick = { position ->
//            // 存储长按位置用于创建菜单
//            longClickedPosition = position
//        }

//        // 设置长按监听
//        adapter?.longClickListener = object : ChatAdapter.OnItemLongClickListener {
//            override fun onItemLongClick(position: Int, view: View) {
//                longClickedPosition = position
//                // 在正确的视图上显示上下文菜单
//                view.showContextMenu()
//            }
//        }


        // 设置长按监听
        // 替换自定义的RecyclerItemClickListener
        recyclerView?.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            private val gestureDetector = GestureDetector(
                recyclerView?.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onLongPress(e: MotionEvent) {
                        val childView = recyclerView?.findChildViewUnder(e.x, e.y)
                        childView?.let {
                            val position = (recyclerView?.getChildAdapterPosition(it)) ?: 0
                            if (position != RecyclerView.NO_POSITION) {
                                longClickedPosition = position

//                            // 关键：创建并设置菜单信息
//                            val menuInfo = object : ContextMenu.ContextMenuInfo {
//                                val adapterPosition = position
//                            }
//
//                            // 使用反射设置菜单信息（Android标准方式）
//                            try {
//                                val field = View::class.java.getDeclaredField("mContextMenuInfo")
//                                field.isAccessible = true
//                                field.set(it, menuInfo)
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                            }

//                            it.showContextMenu()
                            }
                        }
                    }
                })

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

//        // 添加菜单状态监听
//        recyclerView?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
//            override fun onViewAttachedToWindow(v: View) {}
//
//            override fun onViewDetachedFromWindow(v: View) {
//                // 当视图分离时关闭菜单
//                if (isContextMenuShowing) {
//                    closeContextMenu()
//                }
//            }
//        })


        // 创建录音目录
        outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recordings")
        if (!outputDir!!.exists()) outputDir!!.mkdirs()


        audioRecorder = AudioRecorder()


        // 加载历史消息
        loadAndRefreshHistoryMessages()

        // 监听设置变化
        setupPreferenceListener()
//        btnRecord?.setOnClickListener(View.OnClickListener { v: View? -> toggleRecording() })

    }

    private fun clearHistoryData() {
        // 示例：显示确认对话框
        AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空所有历史数据吗？")
            .setPositiveButton("确定") { _, _ ->
                // 实际清空操作，例如：
                // 1. 清除数据库记录
                deleteAllMessage()
                adapter?.deleteAllMessage()
                Toast.makeText(this, "历史数据已清空", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }


    // 添加触摸事件拦截
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isContextMenuShowing) {
            // 检查触摸事件是否在菜单区域内
            val menuView = lastMenuView ?: return super.dispatchTouchEvent(ev)

            val location = IntArray(2)
            menuView.getLocationOnScreen(location)
            val x = location[0]
            val y = location[1]
            val width = menuView.width
            val height = menuView.height

            // 如果触摸在菜单项区域外，关闭菜单
            if (ev.rawX < x || ev.rawX > x + width || ev.rawY < y || ev.rawY > y + height) {
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    closeContextMenu()
                }
                return true // 拦截事件
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
//        adapter?.let {
//            if (v is RecyclerView)
//                if (longClickedPosition != RecyclerView.NO_POSITION) {
//                    val message = it.messages[longClickedPosition]
//                    menuInflater.inflate(R.menu.context_menu, menu)
//
//                    // 动态设置菜单项
//                    menu.findItem(R.id.menu_mark).isVisible = !message.isBookmarked
//                    menu.findItem(R.id.menu_unmark).isVisible = message.isBookmarked
//                }
//
//        }


        if (v is RecyclerView) {
            // 获取位置信息
            val position = when {
                menuInfo != null -> {
                    // 从自定义菜单信息获取位置
                    try {
                        val field = menuInfo.javaClass.getDeclaredField("adapterPosition")
                        field.isAccessible = true
                        field.getInt(menuInfo)
                    } catch (e: Exception) {
                        RecyclerView.NO_POSITION
                    }
                }

                longClickedPosition != -1 -> longClickedPosition
                else -> RecyclerView.NO_POSITION
            }

            if (position != RecyclerView.NO_POSITION) {
                val message = messages[position]
                menuInflater.inflate(R.menu.context_menu, menu)

                // 动态设置菜单项
                val markItem = menu.findItem(R.id.menu_mark)
                val unmarkItem = menu.findItem(R.id.menu_unmark)

                if (message.isBookmarked) {
                    markItem.isVisible = false
                    unmarkItem.isVisible = true
                } else {
                    markItem.isVisible = true
                    unmarkItem.isVisible = false
                }
            }
        }

    }

//    override fun onContextMenuClosed(menu: Menu) {
//        super.onContextMenuClosed(menu)
//        isContextMenuShowing = false // 菜单关闭时更新状态
//        lastMenuView = null // 清除视图引用
//    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        // 获取位置信息
        val position = try {
            val menuInfo = item.menuInfo
            val field = menuInfo!!.javaClass.getDeclaredField("adapterPosition")
            field.isAccessible = true
            field.getInt(menuInfo)
        } catch (e: Exception) {
            longClickedPosition
        }


        if (position == RecyclerView.NO_POSITION) return false

        return when (item.itemId) {
            R.id.menu_copy -> {
                copyMessage(position)
                true
            }

            R.id.menu_select_copy -> {
                startMultiSelectMode(position, selectedPosition)
                true
            }

            R.id.menu_multi_select -> {
                startActionMode(position)
                true
            }

            R.id.menu_mark -> {
                adapter?.markItem(longClickedPosition)
                markMessage(messages, longClickedPosition, true)
                true
            }

            R.id.menu_unmark -> {
                adapter?.unMarkItem(longClickedPosition)
                markMessage(messages, longClickedPosition, false)
                true
            }

            R.id.menu_delete -> {
                deleteMessageByPos(longClickedPosition)
                adapter?.deleteMessageAndNotify(position)
                true
            }

            R.id.menu_share -> {
                shareMessage(longClickedPosition)
                true
            }

            else -> super.onContextItemSelected(item)
        }

    }


    private fun deleteMessageByPos(position: Int) {
        deleteMessageInDb(messages[position])
        adapter?.deleteMessageInAdapter(position)
    }
//    // 处理菜单点击事件
//    override fun onContextItemSelected(item: MenuItem): Boolean {
//        return if (longClickedPosition != RecyclerView.NO_POSITION) {
//            val handled = adapter?.handleContextMenuAction(item.itemId, longClickedPosition)
//            longClickedPosition = RecyclerView.NO_POSITION
//            handled==true
//        } else {
//            super.onContextItemSelected(item)
//        }
//    }


    //    override fun onContextItemSelected(item: MenuItem): Boolean {
//
//        if (!isContextMenuShowing) return false
//        // 获取位置信息
//        val position = try {
//            val menuInfo = item.menuInfo
//            val field = menuInfo!!.javaClass.getDeclaredField("adapterPosition")
//            field.isAccessible = true
//            field.getInt(menuInfo)
//        } catch (e: Exception) {
//            longClickedPosition
//        }
//
//        if (position == RecyclerView.NO_POSITION) return false
//        val message = messages[position]
//        if (longClickedPosition == -1) return false
////        val message = adapter?.messages?.get(longClickedPosition)
//        return when (item.itemId) {
//            R.id.menu_delete -> {
//                adapter?.deleteMessage(longClickedPosition)
//                true
//            }
//            R.id.menu_multi_select -> {
//                adapter?.enableMultiSelectMode(longClickedPosition)
//                true
//            }
//            R.id.menu_mark -> {
//                adapter?.toggleMark(longClickedPosition)
//                true
//            }
//            R.id.menu_copy -> {
//                copyText(message?.content)
//                closeContextMenu() // 操作后关闭菜单
//                true
//            }
//            R.id.menu_select_copy -> {
//                startTextSelection(message?.content, selectedPosition)
//                closeContextMenu() // 操作后关闭菜单
//                true
//            }
//            R.id.menu_share -> {
//                shareText(message?.content)
//                closeContextMenu() // 操作后关闭菜单
//                true
//            }
//            else -> super.onContextItemSelected(item)
//        }
//    }
    // 复制文本方法
    private fun copyText(text: String?) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    // 分享文本方法
    private fun shareText(text: String?) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "分享消息"))
    }

    fun copyMessage(position: Int) {
        var message = adapter?.messages?.get(position)
        copyText(message?.content)
    }

    fun shareMessage(position: Int) {
        // 分享文本
        var message = adapter?.messages?.get(position)
        val shareIntent =
            Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_TEXT, message?.content)
        this.startActivity(Intent.createChooser(shareIntent, "分享消息"))
    }

    fun startMultiSelectMode(position: Int, selectedPosition: Int) {
        var message = adapter?.messages?.get(position)
        startTextSelection(message?.content, selectedPosition)
    }

    // 启动文本选择方法
    private fun startTextSelection(text: String?, position: Int) {
        val intent = Intent(this, TextSelectionActivity::class.java).apply {
            putExtra(TextSelectionActivity.EXTRA_TEXT, text)
            putExtra("position", position)
        }
        startActivity(intent)
    }

    // 启动多选模式
    private fun startActionMode(position: Int) {
        adapter?.enableMultiSelectMode(position)
        actionMode = startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menuInflater.inflate(R.menu.multi_select_menu, menu)
                return true
            }

            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.action_delete -> {
                        deleteSelectedMessages{
                            mode.finish()
                        }
                        return true
                    }
                    R.id.select_all -> {
                        selectAllMessages()
                        return true
                    }
                    R.id.action_mark -> {
                        bookmarkSelectedMessages(true)
                        mode.finish()
                        return true
                    }
                    R.id.action_unmark -> {
                        bookmarkSelectedMessages(false)
                        mode.finish()
                        return true
                    }
                    else -> return false
                }

            }

            override fun onDestroyActionMode(mode: ActionMode) {
//                adapter?.disableMultiSelectMode()
                adapter?.clearSelection()
                actionMode = null
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val count = adapter?.getSelectedCount()
                menu.findItem(R.id.action_count).title = "$count 已选"
                return true
            }
        })
    }

    private fun bookmarkSelectedMessages(check: Boolean) {
        if (check) {
            adapter?.selectedStream()?.forEach(Consumer { pos ->
                markMessage(messages, pos, true)
                adapter?.markItem(pos)
            })

        } else {
            adapter?.selectedStream()?.forEach(Consumer { pos ->
                markMessage(messages, pos, false)
                adapter?.unMarkItem(pos)
            })
        }
    }

    private fun selectAllMessages() {
        // 1. 安全获取消息列表大小
        val size = adapter?.messages?.size ?: 0

        // 2. 批量选中（优化版）
        (0 until size).forEach { position ->
            adapter?.selectItem(position)
        }
    }

    private fun performDeletion(positions: List<Int>) {
        // 1. 批量删除消息
        positions.forEach { pos ->
            deleteMessageByPos(pos)  // 原有删除方法
        }

        // 2. 通知Adapter更新
        when (positions.size) {
            1 -> adapter?.notifyItemRemoved(positions.first())
            else -> {
                // 批量删除时使用范围更新
                val firstPosition = positions.minOrNull() ?: return
                val lastPosition = positions.maxOrNull() ?: return
                adapter?.notifyItemRangeRemoved(firstPosition, positions.size)
            }
        }

        // 3. 清除选中状态
        adapter?.clearSelection()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun deleteSelectedMessages(onDeleteConfirmed: () -> Unit = {}) {
        val positions = adapter?.selectedStream()?.toList()?.sortedDescending() ?: return

        // 添加确认对话框
        AlertDialog.Builder(this).apply {
            setTitle("确认删除")
            setMessage("确定要删除选中的 ${positions.size} 条消息吗？")
            setPositiveButton("删除") { _, _ ->
                performDeletion(positions)  // 用户确认后执行删除
                onDeleteConfirmed()  // 执行回调
            }
            setNegativeButton("取消", null)
            create()
        }.show()
    }


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupPreferenceListener() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPref.registerOnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "enable_file_upload" -> {
                    isFileUploadMode = sharedPref.getBoolean(key, false)
                    updateRecordButtonBehavior()
                }
            }
        }

        // 初始化状态
        isFileUploadMode = sharedPref.getBoolean("enable_file_upload", false)
        updateRecordButtonBehavior()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun updateRecordButtonBehavior() {
        val recordButton: Button = findViewById(R.id.btn_record)

        if (isFileUploadMode) {
            recordButton.text = "上传录音文件"
            recordButton.setOnClickListener {
                openFilePicker()
            }
        } else {
            recordButton.text = "开始录音"
            recordButton.setOnClickListener { v: View? ->
                toggleRecording()
            }
        }
    }

    private fun openFilePicker() {
        // 创建文件选择 Intent
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"  // 只显示音频文件
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)  // 只允许本地文件

            // 设置初始目录
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(outputDir))
        }

        startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 获取选择的文件
                val file = uriToFile(uri)
                val recordingMsg: Message = Message()
                recordingMsg.content = "[录音文件]"
                recordingMsg.isUser = true
                recordingMsg.timestamp = System.currentTimeMillis()
                addMessage(recordingMsg)
                if (file != null) {
                    // 发送到服务器
                    sendToServer(file, recordingMsg.id)
                } else {
                    Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            // 检查是否是 content URI
            if (uri.scheme == "content") {
                // 使用 ContentResolver 读取文件
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    // 创建临时文件
                    val file = File.createTempFile("upload_", ".wav", cacheDir)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    file
                }
            } else {
                // 直接处理 file URI
                File(uri.path ?: return null)
            }
        } catch (e: Exception) {
            Log.e("FilePicker", "Error converting URI to file", e)
            null
        }
    }

    // 关键方法：设置状态栏占位高度
    private fun setStatusBarHeight() {
        // 获取状态栏高度
        val statusBarHeight = getStatusBarHeight()


        // 设置占位View的高度
        val params: ViewGroup.LayoutParams = statusBarBackground!!.layoutParams
        params.height = statusBarHeight / 2
        statusBarBackground!!.layoutParams = params
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

    override fun onBackPressed() {
        if (drawerLayout!!.isDrawerOpen(GravityCompat.END)) {
            drawerLayout!!.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun toggleRecording() {
        if (btn_record!!.text == "开始录音") {
            startRecording()
        } else {
            stopRecording()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        try {
            audioRecorder?.startRecording(outputDir)
            btn_record!!.text = "停止录音"
        } catch (e: IOException) {
            e.printStackTrace()
            addSystemMessage("录音启动失败: " + e.message)
        }
    }

    private fun stopRecording() {
        val audioFile: File? = audioRecorder?.stopRecording()
        btn_record!!.text = "开始录音"


        // 添加用户录音消息
        val recordingMsg: Message = Message()
        recordingMsg.content = "[语音消息]"
        recordingMsg.isUser = true
        recordingMsg.timestamp = System.currentTimeMillis()
        addMessage(recordingMsg)


        // 发送到服务器
        if (audioFile != null) {
            sendToServer(audioFile, recordingMsg.id)

        }
    }

    private fun sendToServer(audioFile: File, ref_id: Int) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val serverUrl = sharedPref.getString("server_url", "http://192.168.1.8:8484/recognize")
            ?: "http://192.168.1.8:8484/recognize"
        NetworkUtils.sendAudioToServer(
            audioFile,
            serverUrl,
            object : NetworkUtils.TranscriptionCallback {
                override fun onSuccess(result: String?) {
                    // 添加系统回复
                    val responseMsg: Message = Message()
                    responseMsg.content = result
                    responseMsg.isUser = false
                    responseMsg.timestamp = System.currentTimeMillis()
                    responseMsg.ref_id = ref_id;
                    addMessage(responseMsg)
                    audioFile.delete()
                }

                override fun onError(error: String?) {
                    addSystemMessage("转换失败: $error")
                    audioFile.delete()
                }


            })
    }

    private fun addMessage(message: Message) {
        runOnUiThread {
            messages.add(message)
            adapter?.notifyItemInserted(messages.size - 1)
            recyclerView!!.scrollToPosition(messages.size - 1)


            // 保存到数据库
            executor.execute { db?.messageDao()?.insert(message) }
        }
    }

    private fun deleteMessageInDb(message: Message) {
        runOnUiThread {
            executor.execute {
                db?.messageDao()?.delete(message)
            }
        }
    }

    private fun deleteAllMessage() {
        runOnUiThread {
            executor.execute {
                db?.messageDao()?.deleteAll()
            }
        }
    }

    private fun markMessage(messages: MutableList<Message>, pos: Int, mark: Boolean?) {
        messages[pos].isBookmarked = mark ?: false;
        updateMessage(messages[pos])
    }

    private fun updateMessage(message: Message) {
        runOnUiThread {
            adapter?.notifyItemChanged(messages.indexOf(message))
            recyclerView!!.scrollToPosition(messages.indexOf(message))


            // 保存到数据库
            executor.execute { db?.messageDao()?.update(message) }
        }
    }

    private fun addSystemMessage(text: String) {
        val message: Message = Message()
        message.content = text
        message.isUser = false
        message.timestamp = System.currentTimeMillis()
        addMessage(message)
    }

    private fun loadAndRefreshHistoryMessages() {
        executor.execute {
            val history: List<Message>? = db?.messageDao()?.getAllMessages()
            runOnUiThread {
                messages.clear()
                if (history != null) {
                    messages.addAll(history)
                }
                adapter?.notifyDataSetChanged()
                recyclerView!!.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (!checkPermissions()) {
                finish() // 权限未获取则关闭应用
            }
        }
    }

    companion object {
        private var longClickedPosition = RecyclerView.NO_POSITION
        private const val REQUEST_PERMISSIONS = 2
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onResume() {
        super.onResume()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val latest = sharedPref.getBoolean("enable_file_upload", false)

        if (latest != isFileUploadMode) {
            isFileUploadMode = latest
            updateRecordButtonBehavior()
        }
    }

    // 在点击消息项时处理多选
    fun onMessageItemClick(position: Int) {
        if (actionMode != null) { // 多选模式中
            if (adapter?.isSelected(position) == true) {
                adapter?.deselectItem(position)
//                if (adapter?.getSelectedCount() == 0) {
//                    actionMode?.finish()
//                }
            } else {
                adapter?.selectItem(position)
            }
        } else {
            // 正常点击处理
        }
    }

    override fun onMultiSelectModeStarted() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.multi_select_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val countItem = menu.findItem(R.id.action_count)
            countItem.title = "${adapter?.getSelectedCount()} 已选"
            return true
        }

        fun deleteUnmarkedMessages(context: Context) {
            val unmarkedPositions = mutableListOf<Int>()

            for (i in 0 until (adapter?.itemCount ?: 0)) {
                if (adapter?.isMarked(i) != true) {
                    unmarkedPositions.add(i)
                }
            }

            // 按降序删除
            unmarkedPositions.sortDescending()
            unmarkedPositions.forEach { position ->
                adapter?.deleteMessageAndNotify(position)
            }

            Toast.makeText(
                context,
                "已删除 ${unmarkedPositions.size} 条未标记消息",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    adapter?.deleteSelectedItems()
                    mode.finish()
                    true
                }

                R.id.action_mark -> {
                    adapter?.markSelectedItems()
                    mode.finish()
                    true
                }

                R.id.select_all -> {
                    adapter?.markSelectedItems()
                    mode.finish()
                    true
                }

                R.id.action_unmark -> {
                    adapter?.unmarkSelectedItems()
                    mode.finish()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter?.clearSelection()
            actionMode = null
        }
    }

}