package com.bigbirdbrother.recordaudio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder


class AudioRecorder {
//    private var mediaRecorder: MediaRecorder? = null
//    private var outputFile: String? = null
//
//    @Throws(IOException::class)
//    fun startRecording(outputDir: File?) {
//        outputFile =
//            File(outputDir, "recording_" + System.currentTimeMillis() + ".wav").absolutePath
//
//        mediaRecorder = MediaRecorder()
//        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
//        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//        mediaRecorder!!.setOutputFile(outputFile)
//
//        mediaRecorder!!.prepare()
//        mediaRecorder!!.start()
//    }
//
//    fun stopRecording(): File {
//        if (mediaRecorder != null) {
//            mediaRecorder!!.stop()
//            mediaRecorder!!.release()
//            mediaRecorder = null
//        }
//        return File(outputFile)
//    }

    private val SAMPLE_RATE: Int = 16000 // 16kHz
    private val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Throws(IOException::class)
    fun startRecording(outputDir: File?) {
        var bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2 // 默认缓冲区大小
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
            throw IOException("无法初始化AudioRecord")
        }

        outputFile = File(outputDir, "recording_" + System.currentTimeMillis() + ".wav")
        if (!outputFile!!.createNewFile()) {
            throw IOException("无法创建文件: " + outputFile!!.absolutePath)
        }

        audioRecord!!.startRecording()
        isRecording = true

        recordingThread = Thread {
            writeAudioDataToFile(outputFile!!, bufferSize)
        }
        recordingThread!!.start()
    }

    private fun writeAudioDataToFile(file: File, bufferSize: Int) {
        try {
            FileOutputStream(file).use { fos ->
                // 写入临时的WAV头（稍后会替换）
                val header = ByteArray(44)
                fos.write(header)

                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord!!.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        fos.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecording(): File? {
        isRecording = false

        if (audioRecord != null) {
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
        }

        if (recordingThread != null) {
            try {
                recordingThread!!.join()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }


        // 添加正确的WAV文件头
        addWavHeader(outputFile!!)

        return outputFile
    }

    private fun addWavHeader(file: File) {
        val totalDataLen = (file.length() - 44).toInt()
        val sampleRate = SAMPLE_RATE
        val channels = 1 // 单声道
        val bitPerSample = 16 // 16位

        try {
            RandomAccessFile(file, "rw").use { wavFile ->
                // RIFF头
                wavFile.write("RIFF".toByteArray()) // ChunkID
                wavFile.write(intToByteArray(totalDataLen + 36), 0, 4) // ChunkSize
                wavFile.write("WAVE".toByteArray()) // Format


                // fmt子块
                wavFile.write("fmt ".toByteArray()) // Subchunk1ID
                wavFile.write(intToByteArray(16), 0, 4) // Subchunk1Size (16 for PCM)
                wavFile.write(shortToByteArray(1.toShort()), 0, 2) // AudioFormat (1 = PCM)
                wavFile.write(shortToByteArray(channels.toShort()), 0, 2) // NumChannels
                wavFile.write(intToByteArray(sampleRate), 0, 4) // SampleRate
                wavFile.write(
                    intToByteArray(sampleRate * channels * bitPerSample / 8),
                    0,
                    4
                ) // ByteRate
                wavFile.write(
                    shortToByteArray((channels * bitPerSample / 8).toShort()),
                    0,
                    2
                ) // BlockAlign
                wavFile.write(shortToByteArray(bitPerSample.toShort()), 0, 2) // BitsPerSample


                // data子块
                wavFile.write("data".toByteArray()) // Subchunk2ID
                wavFile.write(intToByteArray(totalDataLen), 0, 4) // Subchunk2Size
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}