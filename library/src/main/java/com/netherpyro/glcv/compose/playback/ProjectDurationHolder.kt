package com.netherpyro.glcv.compose.playback

import kotlin.math.max

/**
 * @author mmikhailov on 05.05.2020.
 */
class ProjectDurationHolder {

    var projectDuration: Long = 0
        private set

    fun newSequence(startDelaysMs: Long, beginClipAmountMs: Long, durationMs: Long) {
        val endTimeMs = startDelaysMs + beginClipAmountMs + durationMs
        projectDuration = max(projectDuration, endTimeMs)
    }

    fun reset() {
        projectDuration = 0
    }
}