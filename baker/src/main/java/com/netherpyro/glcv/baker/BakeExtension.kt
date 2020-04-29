package com.netherpyro.glcv.baker

import android.content.Context
import android.content.Intent
import android.os.Build
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_BIT_RATE
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_FPS
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_I_FRAME_INTERVAL_SEC
import com.netherpyro.glcv.baker.encode.VideoEncoderCore.Companion.DEFAULT_SIDE_MIN_SIZE
import com.netherpyro.glcv.compose.Composer

/**
 * @author mmikhailov on 01.04.2020.
 */
// todo resolve result video frame rate issue when encoding without service
fun Composer.renderToVideoFile(
        context: Context,
        outputPath: String,
        outputMinSidePx: Int = DEFAULT_SIDE_MIN_SIZE,
        fps: Int = DEFAULT_FPS,
        iFrameIntervalSecs: Int = DEFAULT_I_FRAME_INTERVAL_SEC,
        bitRate: Int = DEFAULT_BIT_RATE,
        verboseLogging: Boolean = false,
        progressListener: ((progress: Float, completed: Boolean) -> Unit)? = null
): Cancellable =
        Baker.bake(
                viewportColor,
                takeTemplate(),
                outputPath,
                outputMinSidePx,
                fps,
                iFrameIntervalSecs,
                bitRate,
                context,
                getSharedEglContext(),
                progressListener?.let { BakeProgressPublisherSync(it) }
        ).also { Baker.VERBOSE_LOGGING = verboseLogging }

/**
 * Note: register [BakeProgressReceiver] to get bake progress
 * */
fun Composer.renderToVideoFileInSeparateProcess(
        context: Context,
        outputPath: String,
        outputMinSidePx: Int = DEFAULT_SIDE_MIN_SIZE,
        fps: Int = DEFAULT_FPS,
        iFrameIntervalSecs: Int = DEFAULT_I_FRAME_INTERVAL_SEC,
        bitRate: Int = DEFAULT_BIT_RATE,
        verboseLogging: Boolean = false
): Cancellable {
    val data = BakeData(
            viewportColor,
            takeTemplate(),
            outputPath,
            outputMinSidePx,
            fps,
            iFrameIntervalSecs,
            bitRate,
            verboseLogging
    )

    val intent = Intent(context, BakerService::class.java)
        .apply {
            action = BakerService.ACTION_BAKE
            putExtra(BakerService.KEY_BAKE_DATA, data)
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }

    return object : Cancellable {
        override fun cancel() {
            context.startService(
                    Intent(context, BakerService::class.java)
                        .apply { action = BakerService.ACTION_CANCEL }
            )
        }
    }
}