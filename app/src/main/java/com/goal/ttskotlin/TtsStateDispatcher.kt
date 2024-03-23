package com.tensorspeech.tensorflowtts.dispatcher

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.goal.ttskotlin.OnTtsStateListener
import java.util.concurrent.CopyOnWriteArrayList

class TtsStateDispatcher private constructor() {
    companion object {
        private val TAG = "TtsStateDispatcher"
        @Volatile
        private var instance: TtsStateDispatcher? = null
        private val INSTANCE_WRITE_LOCK = Any()

        fun getInstance(): TtsStateDispatcher {
            return instance ?: synchronized(INSTANCE_WRITE_LOCK) {
                instance ?: TtsStateDispatcher().also { instance = it }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val mListeners = CopyOnWriteArrayList<OnTtsStateListener>()

    fun release() {
        Log.d(TAG, "release: ")
        mListeners.clear()
    }

    fun addListener(listener: OnTtsStateListener) {
        if (!mListeners.contains(listener)) {
            Log.d(TAG, "addListener: ${listener.javaClass}")
            mListeners.add(listener)
        }
    }

    fun removeListener(listener: OnTtsStateListener) {
        if (mListeners.contains(listener)) {
            Log.d(TAG, "removeListener: ${listener.javaClass}")
            mListeners.remove(listener)
        }
    }

    fun onTtsStart(text: String) {
        Log.d(TAG, "onTtsStart: ")
        if (mListeners.isNotEmpty()) {
            for (listener in mListeners) {
                handler.post { listener.onTtsStart(text) }
            }
        }
    }

    fun onTtsStop() {
        Log.d(TAG, "onTtsStop: ")
        if (mListeners.isNotEmpty()) {
            for (listener in mListeners) {
                handler.post { listener.onTtsStop() }
            }
        }
    }

    fun onTtsReady() {
        Log.d(TAG, "onTtsReady: ")
        if (mListeners.isNotEmpty()) {
            for (listener in mListeners) {
                handler.post { listener.onTtsReady() }
            }
        }
    }
}
