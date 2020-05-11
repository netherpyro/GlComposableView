package com.netherpyro.glcv.compose.playback

/**
 * @author mmikhailov on 11.05.2020.
 */
interface IPlaybackController {

    fun play()
    fun pause()
    fun seek(ms: Long)
    fun release()
    fun isPlaying(): Boolean
    fun togglePlay()
    fun getCurrentPosition(): Long
    fun setPlaybackEventsListener(listener: PlaybackEventListener?)
}