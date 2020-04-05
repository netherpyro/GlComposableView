package com.netherpyro.glcv.compose.media

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build

/**
 * @author mmikhailov on 04.04.2020.
 */
object Util {

    fun getBitmap(context: Context, uri: Uri): Bitmap {
        val bitmap: Bitmap
        var fd: AssetFileDescriptor? = null

        try {
            fd = context.contentResolver?.openAssetFileDescriptor(uri, "r")

            if (fd == null) {
                throw NullPointerException("File descriptor returned null")
            }

            bitmap = BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, null)
        } finally {
            fd?.close()
        }

        return bitmap
    }

    @Throws(Exception::class)
    fun getMetadata(context: Context, uri: Uri, defaultImageDuration: Long): Metadata {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        val type: Type
        val width: Int
        val height: Int
        val duration: Long
        val orientation: Orientation

        try {
            mediaMetadataRetriever.setDataSource(context, uri)

            type = when {
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                    ?.toBoolean() == true -> Type.VIDEO
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE)
                    ?.toBoolean() == true -> Type.IMAGE
                else -> throw Exception("Cannot detect known media format")
            }

            when (type) {
                Type.VIDEO -> {
                    width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        .toInt()

                    height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        .toInt()

                    orientation = mediaMetadataRetriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        ?.toInt()
                        ?.toOrientation() ?: Orientation.DEG_0

                    duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLong() ?: 0L
                }
                Type.IMAGE -> {
                    width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH)
                            .toInt()
                    } else -1

                    height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT)
                            .toInt()
                    } else -1

                    orientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_ROTATION)
                            ?.toInt()
                            ?.toOrientation()
                            ?: Orientation.DEG_0
                    } else Orientation.DEG_0

                    duration = defaultImageDuration
                }
            }
        } finally {
            mediaMetadataRetriever.release()
        }

        return Metadata(type, width, height, duration, orientation)
    }
}