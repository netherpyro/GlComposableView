package com.netherpyro.glcv.compose.playback

/**
 * @author mmikhailov on 11.05.2020.
 */
interface PlaybackEventListener {

    fun onProgress(positionMs: Long)
    fun onIsPlayingChanged(isPlaying: Boolean)
    fun onPlaybackEnded()
}