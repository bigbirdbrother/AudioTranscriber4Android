package com.bigbirdbrother.recordaudio

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room.databaseBuilder
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var btnRecord: Button? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: ChatAdapter? = null
    private val messages: MutableList<Message> = ArrayList<Message>()
    private var audioRecorder: AudioRecorder? = null
    private var outputDir: File? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var db: AppDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查权限
//        if (!checkPermissions()) {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(
//                    Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
//                ), REQUEST_PERMISSIONS
//            )
//        }

        try {
            // 确保上下文是 Activity
            if (this !is Activity) {
                Log.e("Permissions", "不是 Activity 实例，不能请求权限")
                return
            }

// 打印权限数组，确认无误
//            var permissions = arrayOf(
//                Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
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


//            if (!checkPermissions()) {
//                ActivityCompat.requestPermissions(
//                    this,permissions , REQUEST_PERMISSIONS
//                )
//            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 这里可以弹个提示，或者做其他逻辑处理
        }




        // 初始化数据库
        db = databaseBuilder(
            applicationContext, AppDatabase::class.java, "chat-database"
        ).build()


        // 初始化UI
        btnRecord = findViewById<Button>(R.id.btnRecord)
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

        btnRecord?.setOnClickListener(View.OnClickListener { v: View? -> toggleRecording() })
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun toggleRecording() {
        if (btnRecord!!.text == "录音") {
            startRecording()
        } else {
            stopRecording()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        try {
            audioRecorder?.startRecording(outputDir)
            btnRecord!!.text = "停止录音"
        } catch (e: IOException) {
            e.printStackTrace()
            addSystemMessage("录音启动失败: " + e.message)
        }
    }

    private fun stopRecording() {
        val audioFile: File? = audioRecorder?.stopRecording()
        btnRecord!!.text = "录音"


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
        NetworkUtils.sendAudioToServer(
            audioFile,
            "http://192.168.1.8:8484/recognize",
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