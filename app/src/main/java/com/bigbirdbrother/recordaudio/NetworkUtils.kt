package com.bigbirdbrother.recordaudio

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException


object NetworkUtils {

    interface TranscriptionCallback {
        fun onSuccess(result: String?)
        fun onError(error: String?)
    }

    fun sendAudioToServer(audioFile: File, serverUrl: String, callback: TranscriptionCallback) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", audioFile.name,
                audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            )
            .build()


        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 解析JSON响应
                    val responseBody = response.body!!.string()
                    val json = JSONObject(responseBody)
                    val result = json.getString("result") // 获取result字段
                    callback.onSuccess(result)
                } else {
                    callback.onError("Server error: " + response.code)
                }
            }
        })
    }

}