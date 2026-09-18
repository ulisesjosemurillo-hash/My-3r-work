package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecordManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    var currentlyPlayingPath: String? = null
        private set

    private fun getAudioDirectory(): File {
        val dir = File(context.filesDir, "audio_comments")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startRecording(documentId: String, fragmentId: Int): File? {
        stopPlayback()
        stopRecording()

        val dir = getAudioDirectory()
        val file = File(dir, "audio_${documentId}_${fragmentId}_${System.currentTimeMillis()}.m4a")

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentRecordingFile = file
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
            file
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error al iniciar grabación de audio", e)
            mediaRecorder?.release()
            mediaRecorder = null
            currentRecordingFile = null
            isRecording = false
            null
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        if (!isRecording) return Pair(null, 0)

        val durationSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
        val file = currentRecordingFile

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error al detener grabación", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            currentRecordingFile = null
        }

        return Pair(file, durationSeconds)
    }

    fun cancelRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {}
            mediaRecorder = null
            isRecording = false
            currentRecordingFile?.delete()
            currentRecordingFile = null
        }
    }

    fun playAudio(filePath: String, onCompleted: () -> Unit) {
        stopPlayback()

        val file = File(filePath)
        if (!file.exists()) {
            onCompleted()
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    this@AudioRecordManager.isPlaying = false
                    currentlyPlayingPath = null
                    onCompleted()
                }
                start()
            }
            mediaPlayer = player
            isPlaying = true
            currentlyPlayingPath = filePath
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error al reproducir audio: $filePath", e)
            stopPlayback()
            onCompleted()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        currentlyPlayingPath = null
    }

    fun deleteAudioFile(filePath: String) {
        if (currentlyPlayingPath == filePath) {
            stopPlayback()
        }
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    fun deleteAllAudioForDocument(documentId: String) {
        stopPlayback()
        cancelRecording()
        try {
            val dir = getAudioDirectory()
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith("audio_${documentId}_")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
