package com.netherpyro.glcv.util

import android.os.Handler

/**
 * @author Alexei Korshun on 17.06.2020.
 */
internal class Throttler(private val thresholdMs: Long = 300) {

    companion object {
        private const val WHAT = 7879
    }

    private val handler: Handler = Handler()

    fun publish(block: () -> Unit) {
        if (handler.hasMessages(WHAT)) {
            return
        }

        block.invoke()
        handler.sendMessageDelayed(handler.obtainMessage(WHAT), thresholdMs)
    }

    fun clear() {
        handler.removeCallbacksAndMessages(null) // null means all
    }
}