package com.safehome.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioRecordHelper {

    private const val TAG = "AudioRecordHelper"
    private const val MAX_DURATION_MS = 120_000 // 2분

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentFile: File? = null

    fun startRecording(context: Context): String? {
        if (isRecording) return null

        try {
            // 외부 저장소 Downloads/SafeHome 폴더에 저장
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SafeHome/SOS"
            ).apply { mkdirs() }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
            val file = File(dir, "SOS_$timestamp.m4a")
            currentFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                setMaxDuration(MAX_DURATION_MS)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }
                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "녹음 시작: ${file.absolutePath}")
            return file.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "녹음 시작 실패: ${e.message}")
            return null
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d(TAG, "녹음 완료: ${currentFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "녹음 중지 실패: ${e.message}")
        }
    }

    fun getCurrentFile() = currentFile
    fun isRecording() = isRecording

    fun getRecordings(): List<File> {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SafeHome/SOS"
        )
        return if (dir.exists()) {
            dir.listFiles()
                ?.filter { it.extension == "m4a" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else emptyList()
    }

    fun deleteRecording(file: File): Boolean = file.delete()
}