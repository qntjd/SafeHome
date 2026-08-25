package com.safehome.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoRecordHelper {

    private const val TAG = "VideoRecordHelper"
    private const val MAX_DURATION_MS = 120_000 // 2분

    private var mediaRecorder: MediaRecorder? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var isRecording = false
    private var currentFile: File? = null
    private var appContext: Context? = null

    fun startRecording(context: Context, useFrontCamera: Boolean = false) {
        if (isRecording) return
        appContext = context.applicationContext

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "카메라 권한 없음")
            return
        }

        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SafeHome/SOS"
        ).apply { mkdirs() }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
        val file = File(dir, "SOS_VIDEO_$timestamp.mp4")
        currentFile = file

        backgroundThread = HandlerThread("VideoRecordThread").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(1280, 720)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(3_000_000)
                setOutputFile(file.absolutePath)
                setMaxDuration(MAX_DURATION_MS)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }
                prepare()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder 준비 실패: ${e.message}")
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = findCameraId(cameraManager, useFrontCamera) ?: run {
            Log.e(TAG, "사용 가능한 카메라 없음")
            return
        }

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    Log.e(TAG, "카메라 오류: $error")
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            Log.e(TAG, "카메라 권한 오류: ${e.message}")
        }
    }

    private fun findCameraId(manager: CameraManager, useFrontCamera: Boolean): String? {
        val targetFacing = if (useFrontCamera) CameraCharacteristics.LENS_FACING_FRONT
        else CameraCharacteristics.LENS_FACING_BACK
        return manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == targetFacing
        }
    }

    private fun startCaptureSession() {
        val recorderSurface = mediaRecorder?.surface ?: return
        val camera = cameraDevice ?: return

        try {
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(recorderSurface)
            }

            camera.createCaptureSession(
                listOf(recorderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                        mediaRecorder?.start()
                        isRecording = true
                        Log.d(TAG, "영상 녹화 시작: ${currentFile?.absolutePath}")
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "캡처 세션 구성 실패")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "캡처 세션 시작 실패: ${e.message}")
        }
    }

    fun stopRecording() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null

            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null

            backgroundThread?.quitSafely()
            backgroundThread = null
            backgroundHandler = null

            isRecording = false
            Log.d(TAG, "영상 녹화 완료: ${currentFile?.absolutePath}")
            currentFile?.let { file ->
                appContext?.let { ctx ->
                    android.media.MediaScannerConnection.scanFile(
                        ctx, arrayOf(file.absolutePath), arrayOf("video/mp4"), null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "영상 녹화 중지 실패: ${e.message}")
        }
    }

    fun isRecording() = isRecording
    fun getCurrentFile() = currentFile

    fun getRecordings(): List<File> {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SafeHome/SOS"
        )
        return if (dir.exists()) {
            dir.listFiles()
                ?.filter { it.extension == "mp4" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else emptyList()
    }

    fun deleteRecording(file: File): Boolean = file.delete()
}