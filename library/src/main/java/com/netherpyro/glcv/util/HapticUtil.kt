package com.netherpyro.glcv.util

import android.content.Context
import android.os.Handler
import com.netherpyro.glcv.extensions.vibrate

/**
 * @author Alexei Korshun on 16.06.2020.
 */
internal class HapticUtil(
        private val context: Context
) {

    private val throttler = Throttler(500)

    fun vibrate() {
        throttler.publish { context.vibrate() }
    }

    private class Throttler(private val thresholdMs: Long = 300) {

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

}