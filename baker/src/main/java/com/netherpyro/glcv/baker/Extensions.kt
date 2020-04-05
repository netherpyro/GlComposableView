package com.netherpyro.glcv.baker

import android.content.Context
import com.netherpyro.glcv.baker.encode.EncoderConfig
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_BIT_RATE
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_FPS
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_I_FRAME_INTERVAL_SEC
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.FULL_HD_HEIGHT
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.FULL_HD_WIDTH
import com.netherpyro.glcv.compose.Composer

/**
 * @author mmikhailov on 01.04.2020.
 */
fun Composer.renderToVideoFile(
        context: Context,
        outputPath: String,
        width: Int = FULL_HD_WIDTH,
        height: Int = FULL_HD_HEIGHT,
        fps: Int = DEFAULT_FPS,
        iFrameIntervalSecs: Int = DEFAULT_I_FRAME_INTERVAL_SEC,
        bitRate: Int = DEFAULT_BIT_RATE,
        progressListener: ((progress: Float, completed: Boolean) -> Unit)? = null
): Cancellable = Baker.bake(
        context,
        BakerData(viewportColor, takeTemplate()),
        EncoderConfig(outputPath, width, height, fps, iFrameIntervalSecs, bitRate, getSharedEglContext()),
        progressListener
)