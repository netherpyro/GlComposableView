package com.netherpyro.glcv

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object LibraryHelper {

    private const val video1 = "VFX-debug.mov"
    private const val video2 = "VID.mp4"
    private const val video3 = "Test_Audio_video_sync.mp4"

    fun video1(): Uri {
        return createUriSource(video1)
    }

    fun video2(): Uri {
        return createUriSource(video2)
    }

    fun video3(): Uri {
        return createUriSource(video3)
    }

    @SuppressLint("StaticFieldLeak") // application context used
    private lateinit var sContext: Context

    fun setContext(context: Context) {
        sContext = context
    }

    private fun getEffectFilePath(fileName: String): String? {
        var result: String? = null
        try {
            sContext.assets
                .open(fileName)
                .use { inputStream ->
                    val b = ByteArray(inputStream.available())
                    inputStream.read(b)
                    val effectsFolder = File(
                            sContext.cacheDir.absolutePath)
                    effectsFolder.mkdirs()
                    val effectFile = File(effectsFolder, fileName)
                    val fileOutputStream = FileOutputStream(effectFile)
                    fileOutputStream.write(b)
                    fileOutputStream.close()
                    result = effectFile.path
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return result
    }

    private fun createUriSource(fileName: String): Uri {
        return Uri.parse(getEffectFilePath(fileName))
    }

    /**
     * Checks if the system supports OpenGL ES 2.0.
     */
    fun isGlEs2Supported(context: Context): Boolean {
        val activityManager = context.getSystemService(
                Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        return configurationInfo != null && configurationInfo.reqGlEsVersion >= 0x20000
    }
}