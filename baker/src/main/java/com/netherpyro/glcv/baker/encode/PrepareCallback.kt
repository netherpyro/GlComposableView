package com.netherpyro.glcv.baker.encode

/**
 * @author mmikhailov on 25.04.2020.
 */
internal interface PrepareCallback {
    fun onPrepared(audioBufferProvider: AudioBufferProvider)
}