package com.netherpyro.glcv.baker.decode

import android.util.Log

/**
 * @author mmikhailov on 26.04.2020.
 */
internal class SpeedController(fps: Int) {

    companion object {
        private const val TAG = "SpeedControlCallback"
    }

    private val frameDurationUsec: Long = 1_000_000L / fps

    private var prevMonoUsec: Long = 0

    fun test(currentPtsUsec: Long): Boolean {
        if (prevMonoUsec == 0L) {
            prevMonoUsec = currentPtsUsec
            return true
        }

        val desiredUsec = prevMonoUsec + frameDurationUsec

        Log.v(TAG,
                "test::currentPts=$currentPtsUsec, desired=$desiredUsec, diff=${desiredUsec - currentPtsUsec}")

        if (currentPtsUsec < desiredUsec) {
            return false
        }

        prevMonoUsec += frameDurationUsec
        return true
    }
}