package com.goal.ttskotlin

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import android.Manifest

class MainActivity : ComponentActivity() {
    private lateinit var audioRecordManager: AudioRecordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioRecordManager = AudioRecordManager(this)

        TtsManager.init(this)
        setContent {
            TtsScreen(onStartRecordingClicked = {
                // This will check permissions and start recording accordingly
                checkPermissionsAndStartRecording()
            })
        }
    }
    private fun checkPermissionsAndStartRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            // Permission already granted, start recording
            audioRecordManager.startRecording()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was granted, start recording
                audioRecordManager.startRecording()
            } else {
                // Permission denied, handle as appropriate
            }
        }
    }

    companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}

@Composable
fun TtsScreen(onStartRecordingClicked: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf(1f) } // Placeholder for speed control
    var isRecording by remember { mutableStateOf(false) } // State to track recording status
    val context = LocalContext.current // Access current context
    val audioRecordManager = remember { AudioRecordManager(context) } // Initialize your audio manager
    val outputFile = remember { "${context.externalCacheDir?.absolutePath}/recorded_audio.wav" }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text to Speak") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { TtsManager.speak(text, speed, true) }) {
            Text("Speak")
        }

        Button(onClick = { TtsManager.stopTts() }) {
            Text("Stop")
        }
        Button (onClick = {
            onStartRecordingClicked()
            if (isRecording) {
                audioRecordManager.stopRecording()
            } else {
                audioRecordManager.startRecording()
            }
            isRecording = !isRecording
        }) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}
