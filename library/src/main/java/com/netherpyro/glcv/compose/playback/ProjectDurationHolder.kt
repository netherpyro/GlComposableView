package com.netherpyro.glcv.compose.playback

import kotlin.math.max

/**
 * @author mmikhailov on 05.05.2020.
 */
class ProjectDurationHolder {

    var projectDuration: Long = 0
        private set

    private var changesListener: ((Long) -> Unit)? = null

    fun newSequence(startDelaysMs: Long, beginClipAmountMs: Long, durationMs: Long) {
        val endTimeMs = startDelaysMs + beginClipAmountMs + durationMs
        val oldDuration = projectDuration
        projectDuration = max(projectDuration, endTimeMs)

        if (oldDuration != projectDuration) {
            changesListener?.invoke(projectDuration)
        }
    }

    fun reset() {
        projectDuration = 0
    }

    fun listenChanges(listener: (Long) -> Unit) {
        changesListener = listener
    }
}