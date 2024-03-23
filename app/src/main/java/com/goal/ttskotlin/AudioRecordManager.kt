package com.goal.ttskotlin

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class AudioRecordManager(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // Adjust these values to match your desired audio format
    private val sampleRate = 44100 // Example: CD quality
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun startRecording() = GlobalScope.launch(Dispatchers.IO) {
        Log.d("AudioRecordManager", "Start recording requested")

        if (isRecording) {
            Log.d("AudioRecordManager", "Recording is already in progress")

        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d("AudioRecordManager", "Recording permission not granted")
            // Permission is not granted
        }

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

        audioRecord?.startRecording()
        isRecording = true
        Log.d("AudioRecordManager", "Recording started")

        Thread {
            val data = ByteArrayOutputStream()
            val audioData = ByteArray(bufferSize)

            while (isRecording) {
                val read = audioRecord!!.read(audioData, 0, bufferSize)
                if (read > 0) {
                    data.write(audioData, 0, read)
                }
            }

            val wavData = convertRawToWav(data.toByteArray(), sampleRate)
            val fileName = "Recording_${System.currentTimeMillis()}.wav"

            saveRecordingToMusicFolder(context, wavData, fileName)
            Log.d("AudioRecordManager", "Recording saved: $fileName")

        }.start()
    }
    private fun saveRecordingToMusicFolder(context: Context, wavData: ByteArray, fileName: String) {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val details = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, fileName.substringBeforeLast("."))
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            // Saving in the system's Music directory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
            }
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val audioUri = resolver.insert(collection, details)

        audioUri?.let { uri ->
            resolver.openOutputStream(uri).use { outputStream ->
                outputStream?.write(wavData)
            }
            // Make sure to set IS_PENDING to 0 once done
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                details.clear()
                details.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, details, null, null)
            }
        } ?: run {
            // Handle the error scenario
        }
    }
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun convertRawToWav(rawAudioBytes: ByteArray, sampleRate: Int): ByteArray {
        val headerSize = 44
        val totalDataLen = rawAudioBytes.size + headerSize - 8
        val audioDataLen = rawAudioBytes.size
        val sampleRateInHz = sampleRate
        val channels = 1 // Mono
        val byteRate = sampleRateInHz * channels * 16 / 8

        val header = ByteArray(headerSize)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRateInHz and 0xff).toByte()
        header[25] = (sampleRateInHz shr 8 and 0xff).toByte()
        header[26] = (sampleRateInHz shr 16 and 0xff).toByte()
        header[27] = (sampleRateInHz shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // block align
        header[32] = (2 * 16 / 8).toByte() // 16 bits per sample, 1 channel
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioDataLen and 0xff).toByte()
        header[41] = (audioDataLen shr 8 and 0xff).toByte()
        header[42] = (audioDataLen shr 16 and 0xff).toByte()
        header[43] = (audioDataLen shr 24 and 0xff).toByte()

        return header + rawAudioBytes
    }

}
