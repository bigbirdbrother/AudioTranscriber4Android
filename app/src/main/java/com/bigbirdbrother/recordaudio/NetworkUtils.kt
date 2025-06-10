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
                callback.onError("网络错误: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()

                    if (responseBody.isNullOrEmpty()) {
                        callback.onError("空响应")
                        return
                    }

                    val json = JSONObject(responseBody)

                    // 如果服务器返回了 error 字段
                    if (json.has("error")) {
                        val errorMsg = json.getString("error")
                        callback.onError("服务器错误: $errorMsg")
                        return
                    }

                    val result = json.optString("result", null)

                    if (result == null) {
                        callback.onError("响应中缺少 result 字段")
                    } else {
                        callback.onSuccess(result)
                    }

                } catch (e: Exception) {
                    callback.onError("解析响应失败: ${e.message}")
                }
            }
        })
    }
}
