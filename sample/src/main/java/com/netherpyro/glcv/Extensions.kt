package com.netherpyro.glcv

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * @author mmikhailov on 01.05.2020.
 */
fun <T : View> T.alsoOnLaid(block: (T) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            block.invoke(this@alsoOnLaid)
        }
    })
}

fun Context.attrValue(@AttrRes res: Int): Int {
    val value: Int?
    val tv = TypedValue()
    if (this.theme.resolveAttribute(res, tv, true)) value = tv.data
    else throw Resources.NotFoundException("Resource with id $res not found")
    return value
}

fun Context.dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.coerceAtLeast(0f), resources.displayMetrics)

fun Context.getActionBarSize(): Int {
    var actionBarHeight = 0
    val typedValue = TypedValue()

    try {
        if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        }
    } catch (ignore: Exception) {
    }

    if (actionBarHeight == 0) {
        actionBarHeight = dpToPx(52f).toInt()
    }

    return actionBarHeight
}

fun RecyclerView.addDivider() {
    val decoration =
            DividerItemDecoration(context, LinearLayout.HORIZONTAL)
                .apply { setDrawable(ContextCompat.getDrawable(context, R.drawable.shape_divider)!!) }
    addItemDecoration(decoration)
}

fun Context.saveToGallery(file: File): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(file)
    else saveLegacy(file)
}

@TargetApi(29)
private fun Context.saveScoped(file: File): Uri? {
    val resolver = this.contentResolver
    val volumeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY
    else MediaStore.VOLUME_EXTERNAL
    val videoCollection = MediaStore.Video.Media.getContentUri(volumeName)

    return ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME, file.name) }
        .let { fileDetails -> resolver.insert(videoCollection, fileDetails) }
        ?.apply {
            resolver.openOutputStream(this)
                .use { outputStream -> outputStream?.write(file.readBytes()) }
        }
}

private fun Context.saveLegacy(file: File): Uri? {
    val authority = "${BuildConfig.APPLICATION_ID}.provider"
    val sharedFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            file.name
    ).also { sharedFile -> sharedFile.writeBytes(file.readBytes()) }

    MediaScannerConnection.scanFile(this, arrayOf(sharedFile.absolutePath), arrayOf("video/mp4"), null)
    return FileProvider.getUriForFile(this, authority, sharedFile)
}

fun Context.playVideo(uri: Uri) {
    startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "video/mp4")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}