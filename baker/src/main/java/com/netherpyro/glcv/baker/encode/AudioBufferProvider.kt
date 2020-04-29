package com.netherpyro.glcv.baker.encode

/**
 * @author mmikhailov on 28.04.2020.
 */
internal interface AudioBufferProvider {
    /**
     * @return The Audio buffer where decoder's audio data chunk will be stored
     */
    fun provide(tag: String): AudioBuffer
}