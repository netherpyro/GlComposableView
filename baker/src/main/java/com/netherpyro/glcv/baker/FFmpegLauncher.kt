package com.netherpyro.glcv.baker

import android.content.Context
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.netherpyro.glcv.compose.template.TemplateUnit
import java.io.File

/**
 * @author mmikhailov on 07.10.2020.
 */
object FFmpegLauncher {

    private const val audioOutputKey = "[a]"

    fun putAudio(
            context: Context,
            targetPath: String,
            resultPath: String,
            mediaUnits: List<TemplateUnit>,
            onTimeProgress: (Int) -> Unit,
            onFinish: () -> Unit
    ) {
        val command = arrayOf("-y")
            .plus(buildInputs(context, targetPath, mediaUnits))
            .plus(buildAudioFilter(mediaUnits))
            .plus(buildOutput(resultPath))

        Log.i("FFmpegLauncher", """putAudio::command = 
            ${command.joinToString(separator = " ")}
        """.trimIndent())

        Config.enableStatisticsCallback { onTimeProgress(it.time) }
        FFmpeg.executeAsync(command) { _, _ ->
            Config.enableStatisticsCallback(null)
            onFinish()
        }
    }

    private fun buildInputs(context: Context, targetPath: String, mediaUnits: List<TemplateUnit>): Array<String> {
        val targetInput = arrayOf("-i", targetPath)
        val mediaInputs = mediaUnits
            .asSequence()
            .map { media -> media.toAbsolutePath(context) }
            .flatMap { filePath -> sequenceOf("-i", filePath) }
            .toArray(mediaUnits.size * 2)

        return targetInput + mediaInputs
    }

    private fun buildOutput(resultPath: String) = arrayOf(
            "-map", "0",
            "-map", audioOutputKey,
            "-c:v", "copy",
            resultPath
    )

    private fun buildAudioFilter(inputs: List<TemplateUnit>): Array<String> {
        val sb = StringBuilder()
        val segmentNames = mutableListOf<String>()

        for ((i, media) in inputs.withIndex()) {
            val inputName = "[${i + 1}:a]"
            val outputName = "[a${i + 1}]"

            sb.append("$inputName${trimAndDelayAudio(media)}$outputName;")
            segmentNames.add(outputName)
        }

        sb.append(segmentNames.joinToString(separator = ""))
        sb.append("amix=inputs=${segmentNames.size}$audioOutputKey")

        return arrayOf("-filter_complex", sb.toString())
    }

    private fun trimAndDelayAudio(media: TemplateUnit): String =
            "atrim=start=${media.startClipMs.toSeconds()}:duration=${media.trimmedDurationMs.toSeconds()}" +
                ",adelay=${media.startDelayMs}|${media.startDelayMs}" +
                ",asetpts=PTS-STARTPTS"

    private fun Long.toSeconds() = this / 1000.0

    private fun TemplateUnit.toAbsolutePath(context: Context): String {
        return if (this.uri.scheme != "file") {
            val tempFile = File(context.cacheDir, "temp_${uri.lastPathSegment}.mp4")
            // copy content to temp file (ffmpeg needed absolute path)
            context.contentResolver.openInputStream(uri)
                ?.copyTo(tempFile.outputStream())

            tempFile.absolutePath
        } else {
            this.uri.path!!
        }
    }

    private inline fun <reified T> Sequence<T>.toArray(size: Int): Array<T> {
        val iterator = iterator()
        return Array(size) { iterator.next() }
    }
}