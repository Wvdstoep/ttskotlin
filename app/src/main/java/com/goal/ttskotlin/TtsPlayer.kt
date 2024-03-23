package com.goal.ttskotlin

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue

internal class TtsPlayer {
    private val mAudioTrack: AudioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(22050)
            .setEncoding(FORMAT)
            .setChannelMask(CHANNEL)
            .build(),
        BUFFER_SIZE,
        AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
    )
    private val mAudioQueue = LinkedBlockingQueue<AudioData>()
    private var mCurrentAudioData: AudioData? = null

    init {
        mAudioTrack.play()
        Log.d(TAG, "AudioTrack initialization and playback started")

        ThreadPoolManager.instance.getSingleExecutor("audio")?.execute {
            while (true) {
                try {
                    val currentAudioData = mAudioQueue.take()
                    mCurrentAudioData = currentAudioData // Update the current audio data reference
                    Log.d(TAG, "Playing audio for text: ${currentAudioData.text}")

                    var index = 0
                    while (index < currentAudioData.audio.size && !currentAudioData.isInterrupted()) {
                        val buffer = Math.min(BUFFER_SIZE, currentAudioData.audio.size - index)
                        mAudioTrack.write(currentAudioData.audio, index, buffer, AudioTrack.WRITE_BLOCKING)
                        index += buffer
                        Log.d(TAG, "Audio chunk played")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in audio playback", e)
                }
            }
        }
    }

    fun play(audioData: AudioData) {
        Log.d(TAG, "Adding audio data to queue: ${audioData.text}")
        mAudioQueue.offer(audioData)
    }

    fun interrupt() {
        mAudioQueue.clear()
        mCurrentAudioData?.interrupt()
        Log.d(TAG, "Playback interrupted and queue cleared")
    }

    internal class AudioData(val text: String, val audio: FloatArray) {
        @Volatile private var isInterrupt = false

        fun interrupt() {
            isInterrupt = true
        }

        fun isInterrupted() = isInterrupt
    }

    companion object {
        private const val TAG = "TtsPlayer"
        private const val FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        private const val SAMPLERATE = 22050
        private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        private val BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLERATE, CHANNEL, FORMAT)
    }
}
