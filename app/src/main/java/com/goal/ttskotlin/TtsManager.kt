package com.goal.ttskotlin

import android.content.Context
import android.util.Log
import com.tensorspeech.tensorflowtts.dispatcher.TtsStateDispatcher
import java.io.File
import java.io.FileOutputStream

object TtsManager {
    private var mWorker: InputWorker? = null
    private const val TAG = "TtsManager"
    private const val FASTSPEECH2_MODULE = "fastspeech2_quant.tflite"
    private const val MELGAN_MODULE = "mbmelgan.tflite"

    fun init(context: Context) {
        Log.d(TAG, "Initializing TTS Manager")
        ThreadPoolManager.instance.getSingleExecutor("init")?.execute {
            try {
                val fastspeech = copyFile(context, FASTSPEECH2_MODULE)
                val vocoder = copyFile(context, MELGAN_MODULE)
                mWorker = InputWorker(fastspeech, vocoder)
                Log.d(TAG, "TTS Manager initialized successfully")
                // Signal that TTS is ready to use
                TtsStateDispatcher.getInstance().onTtsReady()
            } catch (e: Exception) {
                Log.e(TAG, "mWorker init failed", e)
            }
        }
    }


    private fun copyFile(context: Context, strOutFileName: String): String {
        val file = File(context.filesDir, strOutFileName)
        if (file.exists()) {
            Log.d(TAG, "File exists: $strOutFileName")
            return file.absolutePath
        }
        context.assets.open(strOutFileName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d(TAG, "Successfully copied file: $strOutFileName")
        return file.absolutePath
    }

    fun stopTts() {
        mWorker?.interrupt()
    }

    fun speak(inputText: String, speed: Float, interrupt: Boolean) {
        Log.d(TAG, "speak called with inputText: $inputText, speed: $speed, interrupt: $interrupt")
        if (interrupt) {
            stopTts()
        }
        ThreadPoolManager.instance.execute {
            Log.d(TAG, "Executing speech task")
            mWorker?.processInput(inputText, speed)
        }
    }

}
