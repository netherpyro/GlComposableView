package com.netherpyro.glcv.baker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * @author mmikhailov on 26.04.2020.
 */
// todo make informative notification with progress and cancel action (customizable)
// todo add option to show finish notification
class BakerService : Service() {

    companion object {
        private const val TAG = "BakerService"
        const val ACTION_BAKE = "com.netherpyro.glcv.baker.ACTION_BAKE"
        const val ACTION_CANCEL = "com.netherpyro.glcv.baker.ACTION_CANCEL"
        const val KEY_BAKE_DATA = "com.netherpyro.glcv.baker.KEY_BAKE_DATA"
    }

    private var bakeProcess: Cancellable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_BAKE -> {
                    val data = intent.extras?.getParcelable(KEY_BAKE_DATA) as? BakeData
                    if (data != null) {
                        doBake(data)
                        startForeground(787980, createNotification())
                    } else {
                        Log.w(TAG, "no data provided")
                        stopSelf()
                    }
                }
                ACTION_CANCEL -> {
                    bakeProcess?.cancel()
                    // todo check needs wait
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun doBake(bakeData: BakeData) {
        bakeProcess = Baker.bake(
                bakeData.viewportColor,
                bakeData.template,
                bakeData.outputPath,
                bakeData.outputMinSidePx,
                bakeData.fps,
                bakeData.iFrameIntervalSecs,
                bakeData.bitRate,
                applicationContext,
                null,
                BakeProgressPublisherAsync(applicationContext, onFinish = { handleFinish() })
        )
            .also { Baker.VERBOSE_LOGGING = bakeData.verboseLogging }
    }

    private fun createNotification(): Notification {
        val title = "Render"
        val icon = R.drawable.ic_stat_name
        val color = Color.BLACK
        val channelId = "BAKE_CHANNEL_ID"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                        NotificationChannel(channelId,
                                "Rendering",
                                NotificationManager.IMPORTANCE_HIGH)
                            .apply { description = "Render process" }
                )
        }

        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setColor(color)
            .setAutoCancel(true)
            .build()
    }

    private fun handleFinish() {
        stopSelf()
    }
}