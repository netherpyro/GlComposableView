package com.netherpyro.glcv.baker

import android.content.Context
import android.content.Intent

/**
 * @author mmikhailov on 26.04.2020.
 */
internal interface BakeProgressPublisher {
    fun publish(encodeTarget: EncodeTarget, progress: Float, completed: Boolean)
}

/**
 * Indicates what is encoding now
 */
enum class EncodeTarget {
    VIDEO, AUDIO
}

internal class BakeProgressPublisherSync(
        private val listener: (encodeTarget: EncodeTarget, progress: Float, completed: Boolean) -> Unit
) : BakeProgressPublisher {

    /**
     * Make sure handle callback in UI thread
     * */
    override fun publish(encodeTarget: EncodeTarget, progress: Float, completed: Boolean) {
        listener(encodeTarget, progress, completed)
    }
}

internal class BakeProgressPublisherAsync(
        private val context: Context,
        private val onFinish: () -> Unit
) : BakeProgressPublisher {
    override fun publish(encodeTarget: EncodeTarget, progress: Float, completed: Boolean) {
        if (completed) onFinish()

        context.sendBroadcast(
                Intent(BakeProgressReceiver.ACTION_PUBLISH_PROGRESS)
                    .apply {
                        putExtra(BakeProgressReceiver.KEY_PROGRESS_ENCODE_TARGET, encodeTarget.ordinal)
                        putExtra(BakeProgressReceiver.KEY_PROGRESS_VALUE, progress)
                        putExtra(BakeProgressReceiver.KEY_PROGRESS_COMPLETED, completed)
                    }
        )
    }
}
