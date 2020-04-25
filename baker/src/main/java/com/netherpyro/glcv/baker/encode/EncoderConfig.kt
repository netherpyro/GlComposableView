package com.netherpyro.glcv.baker.encode

import android.opengl.EGLContext

/**
 * @author mmikhailov on 29.03.2020.
 *
 * Encoder configuration.
 */
data class EncoderConfig(
        val outputPath: String,
        val width: Int,
        val height: Int,
        val fps: Int,
        val iFrameIntervalSecs: Int,
        val bitRate: Int,
        val eglContext: EGLContext?
) {
    override fun toString() = "EncoderConfig: ${width}x${height}@$fps@$bitRate with $iFrameIntervalSecs interval secs to '$outputPath', shared context=$eglContext"
}