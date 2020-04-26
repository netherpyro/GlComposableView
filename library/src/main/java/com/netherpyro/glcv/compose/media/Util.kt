package com.netherpyro.glcv.compose.media

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Size

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
    fun getMetadata(context: Context, uri: Uri, defaultImageDuration: Long = Constant.DEFAULT_IMAGE_DURATION_MS): Metadata {
        val type = getType(context, uri)
        val width: Int
        val height: Int
        val duration: Long
        val orientation: Orientation

        when (type) {
                Type.VIDEO -> {
                    val mediaMetadataRetriever = MediaMetadataRetriever()

                    try {
                        mediaMetadataRetriever.setDataSource(context, uri)

                        width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            .toInt()

                        height = mediaMetadataRetriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            .toInt()

                        orientation = mediaMetadataRetriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                            ?.toInt()
                            ?.toOrientation() ?: Orientation.DEG_0

                        duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLong() ?: 0L

                    } finally {
                        mediaMetadataRetriever.release()
                    }
                }
                Type.IMAGE -> {
                    // todo handle uri of non-content scheme
                    val cursor = context.contentResolver.query(
                            uri,
                            arrayOf(
                                    MediaStore.Files.FileColumns.WIDTH,
                                    MediaStore.Files.FileColumns.HEIGHT,
                                    MediaStore.Files.FileColumns.ORIENTATION
                            ), null, null, null)
                        ?: throw NullPointerException("Cursor returned null")

                    cursor.moveToFirst()

                    width = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH))
                        .toInt()

                    height = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT))
                        .toInt()

                    orientation = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.ORIENTATION))
                        .toInt()
                        .toOrientation()

                    duration = defaultImageDuration

                    cursor.close()
                }
            }

        return Metadata(type, width, height, duration, orientation)
    }

    fun resolveResolution(aspectRatio: Float, sidePx: Int): Size {
        val outputWidth: Int
        val outputHeight: Int

        when {
            aspectRatio > 1f -> {
                outputWidth = (sidePx * aspectRatio).toInt()
                outputHeight = sidePx
            }
            aspectRatio < 1f -> {
                outputWidth = sidePx
                outputHeight = (sidePx * aspectRatio).toInt()
            }
            else -> {
                outputWidth = sidePx
                outputHeight = sidePx
            }
        }

        return Size(outputWidth, outputHeight)
    }

    private fun getType(context: Context, uri: Uri): Type {
        val mimeType = context.contentResolver.getType(uri)

        return when {
            mimeType?.startsWith("image") == true -> Type.IMAGE
            mimeType?.startsWith("video") == true -> Type.VIDEO
            else -> throw Exception("Cannot detect known media format")
        }
    }
}