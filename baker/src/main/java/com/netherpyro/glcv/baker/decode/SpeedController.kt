package com.netherpyro.glcv.baker.decode

import android.util.Log
import com.netherpyro.glcv.baker.Baker

/**
 * @author mmikhailov on 26.04.2020.
 */
internal class SpeedController(fps: Int) {

    companion object {
        private const val TAG = "SpeedControlCallback"
    }

    private val verboseLogging = Baker.VERBOSE_LOGGING

    private val frameDurationUsec: Long = 1_000_000L / fps

    private var prevUsec: Long = 0

    fun test(currentPtsUsec: Long): Boolean {
        if (prevUsec == 0L) {
            prevUsec = currentPtsUsec
            return true
        }

        val desiredUsec = prevUsec + frameDurationUsec

        if (verboseLogging) Log.v(TAG,
                "test::currentPts=$currentPtsUsec, desired=$desiredUsec, diff=${desiredUsec - currentPtsUsec}")

        if (currentPtsUsec < desiredUsec) {
            return false
        }

        prevUsec += frameDurationUsec
        return true
    }
}