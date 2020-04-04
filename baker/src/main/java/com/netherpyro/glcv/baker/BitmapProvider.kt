package com.netherpyro.glcv.baker

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * @author mmikhailov on 04.04.2020.
 */
internal object BitmapProvider {

    fun get(context: Context, uri: Uri): Bitmap {
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
}