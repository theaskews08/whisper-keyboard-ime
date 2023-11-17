package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.IOException
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

private const val RECORDED_AUDIO_FILENAME = "recorded.m4a"

class WhisperInputService : InputMethodService() {
    private var whisperKeyboard: WhisperKeyboard = WhisperKeyboard()
    private var whisperJobManager: WhisperJobManager = WhisperJobManager()
    private var recorderManager: RecorderManager = RecorderManager()
    private var recordedAudioFilename: String = ""

    private fun transcriptionCallback(text: String?) {
        if (!text.isNullOrEmpty()) {
            currentInputConnection?.commitText(text, text.length)
        }

        whisperKeyboard.reset()
    }

    override fun onCreateInputView(): View {
        // Assigns the file name for recorded audio
        recordedAudioFilename = "${externalCacheDir?.absolutePath}/${RECORDED_AUDIO_FILENAME}"

        // Returns the keyboard after setting it up and inflating its layout
        return whisperKeyboard.setup(
            layoutInflater,
            { onStartRecording() },
            { onCancelRecording() },
            { onStartTranscription() },
            { onCancelTranscription() })
    }

    private fun onStartRecording() {
        // Upon starting recording, check whether audio permission is granted.
        if (!recorderManager.allPermissionsGranted(this)) {
            // If not, launch app MainActivity (for permission setup).
            launchMainActivity()
            whisperKeyboard.reset()
            return
        }

        recorderManager.start(this, recordedAudioFilename)
    }

    private fun onCancelRecording() {
        recorderManager.stop()
    }

    private fun onStartTranscription() {
        recorderManager.stop()
        whisperJobManager.startTranscriptionJobAsync(
            this,
            recordedAudioFilename
        ) { transcriptionCallback(it) }
    }

    private fun onCancelTranscription() {
        whisperJobManager.clearTranscriptionJob()
    }

    // Opens up app MainActivity
    private fun launchMainActivity() {
        val dialogIntent = Intent(this, MainActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(dialogIntent)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        whisperJobManager.clearTranscriptionJob()
        whisperKeyboard.reset()
        recorderManager.stop()
    }

    override fun onWindowHidden() {

        super.onWindowHidden()
        whisperJobManager.clearTranscriptionJob()
        whisperKeyboard.reset()
        recorderManager.stop()
    }
}
