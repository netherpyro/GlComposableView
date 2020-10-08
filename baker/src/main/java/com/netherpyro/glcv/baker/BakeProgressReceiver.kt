package com.netherpyro.glcv.baker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * @author mmikhailov on 26.04.2020.
 */
class BakeProgressReceiver(
        private val listener: (encodeTarget: EncodeTarget, progress: Float, completed: Boolean) -> Unit
) : BroadcastReceiver() {

    companion object {
        const val ACTION_PUBLISH_PROGRESS = "com.netherpyro.glcv.baker.ACTION_PUBLISH_PROGRESS"
        const val KEY_PROGRESS_ENCODE_TARGET = "com.netherpyro.glcv.baker.KEY_PROGRESS_ENCODE_TARGET"
        const val KEY_PROGRESS_VALUE = "com.netherpyro.glcv.baker.KEY_PROGRESS_VALUE"
        const val KEY_PROGRESS_COMPLETED = "com.netherpyro.glcv.baker.KEY_PROGRESS_COMPLETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PUBLISH_PROGRESS) {
            val encodeTarget: EncodeTarget = EncodeTarget.values()[intent.extras?.getInt(KEY_PROGRESS_ENCODE_TARGET) ?: 0]
            val value = intent.extras?.getFloat(KEY_PROGRESS_VALUE) ?: 0f
            val completed = intent.extras?.getBoolean(KEY_PROGRESS_COMPLETED) ?: false

            listener(encodeTarget, value, completed)
        }
    }
}