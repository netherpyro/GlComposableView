package com.netherpyro.glcv.baker.encode

import android.opengl.EGLContext
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_BIT_RATE
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_FPS
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_I_FRAME_INTERVAL_SEC
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.FULL_HD_HEIGHT
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.FULL_HD_WIDTH


/**
 * @author mmikhailov on 29.03.2020.
 *
 * Encoder configuration.
 */
class EncoderConfig(
        val outputPath: String,
        val width: Int = FULL_HD_WIDTH,
        val height: Int = FULL_HD_HEIGHT,
        val fps: Int = DEFAULT_FPS,
        val iFrameIntervalSecs: Int = DEFAULT_I_FRAME_INTERVAL_SEC,
        val bitRate: Int = DEFAULT_BIT_RATE,
        val eglContext: EGLContext?
) {
    override fun toString() = "EncoderConfig: ${width}x${height}@$fps@$bitRate with $iFrameIntervalSecs interval secs to '$outputPath', shared context=$eglContext"
}