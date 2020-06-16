package com.netherpyro.glcv.util

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * @author Alexei Korshun on 16.06.2020.
 */
class HapticUtil(
        private val context: Context
) {

    private val throttler = Throttler(500)

    fun vibrate() {
        throttler.publish { context.vibrate() }
    }

}

class Throttler(private val thresholdMs: Long = 300) {

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

fun Context.vibrate(duration: Long = 100) {
    with(getSystemService(Context.VIBRATOR_SERVICE) as Vibrator) {
        if (hasVibrator()) {
            if (is26OrAbove()) {
                vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrate(duration)
            }
        }
    }
}

fun is26OrAbove() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O