package com.netherpyro.glcv.baker

import android.content.Context
import android.content.Intent

/**
 * @author mmikhailov on 26.04.2020.
 */
internal interface BakeProgressPublisher {
    fun publish(progress: Float, completed: Boolean)
}

internal class BakeProgressPublisherSync(
        private val listener: (progress: Float, completed: Boolean) -> Unit
) : BakeProgressPublisher {

    /**
     * Make sure handle callback in UI thread
     * */
    override fun publish(progress: Float, completed: Boolean) {
        listener(progress, completed)
    }
}

internal class BakeProgressPublisherAsync(
        private val context: Context,
        private val onFinish: () -> Unit
) : BakeProgressPublisher {
    override fun publish(progress: Float, completed: Boolean) {
        if (completed) onFinish()

        context.sendBroadcast(
                Intent(BakeProgressReceiver.ACTION_PUBLISH_PROGRESS)
                    .apply {
                        putExtra(BakeProgressReceiver.KEY_PROGRESS_VALUE, progress)
                        putExtra(BakeProgressReceiver.KEY_PROGRESS_COMPLETED, completed)
                    }
        )
    }
}
