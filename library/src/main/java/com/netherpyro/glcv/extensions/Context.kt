package com.netherpyro.glcv.extensions

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * @author Alexei Korshun on 16.06.2020.
 */
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