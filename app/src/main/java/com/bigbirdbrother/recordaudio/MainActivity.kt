package com.bigbirdbrother.recordaudio

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
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


class MainActivity : AppCompatActivity() {
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
                    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
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
        setStatusBarHeight();

        // 设置工具栏
//        setSupportActionBar(toolbar)
        toolbar?.setNavigationOnClickListener { v -> drawerLayout?.openDrawer(GravityCompat.END) }


        // 设置菜单项点击事件
        navigationView?.setNavigationItemSelectedListener { item ->
            if (item.getItemId() === R.id.menu_settings) {
                // 打开设置页面
                startActivity(Intent(this, SettingsActivity::class.java))
                drawerLayout?.closeDrawer(GravityCompat.END)
                return@setNavigationItemSelectedListener true
            }
            false
        }


        // 初始化数据库
        db = databaseBuilder(
            applicationContext, AppDatabase::class.java, "chat-database"
        ).build()


        // 初始化UI
        btn_record = findViewById<Button>(R.id.btn_record)
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ChatAdapter(messages)
        recyclerView?.setLayoutManager(LinearLayoutManager(this))
        recyclerView?.setAdapter(adapter)


        // 创建录音目录
        outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recordings")
        if (!outputDir!!.exists()) outputDir!!.mkdirs()


        audioRecorder = AudioRecorder()


        // 加载历史消息
        loadHistoryMessages()

        // 监听设置变化
        setupPreferenceListener()
//        btnRecord?.setOnClickListener(View.OnClickListener { v: View? -> toggleRecording() })

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
            recordButton.setOnClickListener {
                    v: View? -> toggleRecording()
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
                    sendToServer(file)
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
        params.height = statusBarHeight
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
            sendToServer(audioFile)
        }
    }

    private fun sendToServer(audioFile: File) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val serverUrl = sharedPref.getString("server_url", "http://192.168.1.8:8484/recognize") ?: "http://192.168.1.8:8484/recognize"
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
                    addMessage(responseMsg)
                }

                override fun onError(error: String?) {
                    addSystemMessage("转换失败: $error")
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

    private fun addSystemMessage(text: String) {
        val message: Message = Message()
        message.content = text
        message.isUser = false
        message.timestamp = System.currentTimeMillis()
        addMessage(message)
    }

    private fun loadHistoryMessages() {
        executor.execute {
            val history: List<Message>? = db?.messageDao()?.getAllMessages()
            runOnUiThread {
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
        private const val REQUEST_PERMISSIONS = 2
    }
}