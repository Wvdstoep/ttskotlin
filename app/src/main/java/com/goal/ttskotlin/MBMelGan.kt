package com.goal.ttskotlin

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.FloatBuffer
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

/**
 * @author []" "Xuefeng Ding"">&quot;mailto:xuefeng.ding@outlook.com&quot; &quot;Xuefeng Ding&quot;
 * Created 2020-07-20 17:26
 */
class MBMelGan(modulePath: String?) : AbstractModule() {
    private var mModule: Interpreter? = null

    init {
        try {
            mModule = Interpreter(File(modulePath), option)
            val input = mModule!!.inputTensorCount
            for (i in 0 until input) {
                val inputTensor = mModule!!.getInputTensor(i)
                Log.d(
                    TAG, "input:" + i
                            + " name:" + inputTensor.name()
                            + " shape:" + Arrays.toString(inputTensor.shape()) +
                            " dtype:" + inputTensor.dataType()
                )
            }
            val output = mModule!!.outputTensorCount
            for (i in 0 until output) {
                val outputTensor = mModule!!.getOutputTensor(i)
                Log.d(
                    TAG, "output:" + i
                            + " name:" + outputTensor.name()
                            + " shape:" + Arrays.toString(outputTensor.shape())
                            + " dtype:" + outputTensor.dataType()
                )
            }
            Log.d(TAG, "successfully init")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAudio(input: TensorBuffer): FloatArray {
        mModule!!.resizeInput(0, input.getShape())
        mModule!!.allocateTensors()
        val outputBuffer = FloatBuffer.allocate(350000)
        val time = System.currentTimeMillis()
        mModule!!.run(input.getBuffer(), outputBuffer)
        Log.d(TAG, "time cost: " + (System.currentTimeMillis() - time))
        val audioArray = FloatArray(outputBuffer.position())
        outputBuffer.rewind()
        outputBuffer[audioArray]
        return audioArray
    }
    fun synthesizeAndPlayAudio(input: TensorBuffer) {
        val audioData = getAudio(input)
        playAudio(audioData)
    }
    fun playAudio(audioData: FloatArray) {
        // Assuming the audio data is mono with a sample rate of 44100 Hz
        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBufferSize, audioData.size),
            AudioTrack.MODE_STREAM
        )

        // Convert FloatArray to ShortArray (PCM data)
        val pcmData = ShortArray(audioData.size) { i ->
            // Normalize the float audio data to the range of short values (-32768 to 32767)
            val normalizedSample = (audioData[i] * Short.MAX_VALUE).toInt()
            // Clamp the values to the valid short range
            max(-32768, min(32767, normalizedSample)).toShort()
        }

        audioTrack.play() // Start playback
        audioTrack.write(pcmData, 0, pcmData.size) // Write audio data for playback

        // Don't forget to release the AudioTrack object when done
        audioTrack.release()
    }
    companion object {
        private const val TAG = "MBMelGan"
    }
}
