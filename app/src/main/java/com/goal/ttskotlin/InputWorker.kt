package com.goal.ttskotlin

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.tensorspeech.tensorflowtts.dispatcher.TtsStateDispatcher
import com.tensorspeech.tensorflowtts.utils.Processor
import java.util.concurrent.LinkedBlockingQueue

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class InputWorker(fastspeech: String, vocoder: String) {
    private val TAG = "InputWorker"
    private val mInputQueue = LinkedBlockingQueue<InputText>()
    private var mCurrentInputText: InputText? = null
    private val mFastSpeech2 = FastSpeech2(fastspeech)
    private val mMBMelGan = MBMelGan(vocoder)
    private val mProcessor = Processor()
    private val mTtsPlayer = TtsPlayer()

    init {
        ThreadPoolManager.instance.getSingleExecutor("worker")?.execute {
            while (true) {
                try {
                    mCurrentInputText = mInputQueue.take()
                    Log.d(TAG, "processing: ${mCurrentInputText?.INPUT_TEXT}")
                    TtsStateDispatcher.getInstance().onTtsStart(mCurrentInputText?.INPUT_TEXT ?: "")
                    mCurrentInputText?.proceed()
                    TtsStateDispatcher.getInstance().onTtsStop()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: ", e)
                }
            }
        }
    }

    fun processInput(inputText: String, speed: Float) {
        Log.d(TAG, "add to queue: $inputText")
        mInputQueue.offer(InputText(inputText, speed))
    }

    fun interrupt() {
        mInputQueue.clear()
        mCurrentInputText?.interrupt()
        mTtsPlayer.interrupt()
    }

    private inner class InputText(val INPUT_TEXT: String, val SPEED: Float) {
        var isInterrupt = false
            private set

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun proceed() {
            val sentences = INPUT_TEXT.split("[.,]").toTypedArray()
            Log.d(TAG, "speak: ${sentences.contentToString()}")

            for (sentence in sentences) {
                val time = System.currentTimeMillis()
                val inputIds = mProcessor.textToIds(sentence)
                val output = mFastSpeech2.getMelSpectrogram(inputIds, SPEED)

                if (isInterrupt) {
                    Log.d(TAG, "proceed: interrupt")
                    return
                }

                val encoderTime = System.currentTimeMillis()
                val audioData = mMBMelGan.getAudio(output) // Keep it as FloatArray

                if (isInterrupt) {
                    Log.d(TAG, "proceed: interrupt")
                    return
                }

                val vocoderTime = System.currentTimeMillis()
                Log.d(TAG, "Time cost: ${encoderTime - time}+${vocoderTime - encoderTime}=${vocoderTime - time}")
                mTtsPlayer.play(TtsPlayer.AudioData(sentence, audioData)) // Directly pass FloatArray
            }
        }

        fun interrupt() {
            this.isInterrupt = true
        }
    }
}
