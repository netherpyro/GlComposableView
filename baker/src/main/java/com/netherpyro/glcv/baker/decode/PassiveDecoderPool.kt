package com.netherpyro.glcv.baker.decode

import android.content.Context
import android.net.Uri
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.baker.encode.AudioBufferProvider
import com.netherpyro.glcv.compose.template.TimeMask

/**
 * @author mmikhailov on 04.04.2020.
 */
internal class PassiveDecoderPool {

    private val decoders = mutableMapOf<String, MediaDecoderPassive>()

    fun createMediaDecoder(context: Context, tag: String, uri: Uri, decodeAudioTrack: Boolean, projectFps: Int): SurfaceConsumer {
        val decoder = MediaDecoderPassive(context, tag, uri, decodeAudioTrack, projectFps)
        decoders[tag] = decoder

        return decoder
    }

    fun prepare(audioBufferProvider: AudioBufferProvider) {
        decoders.forEach { it.value.prepare(audioBufferProvider) }
    }

    fun advance(ptsUsec: Long, statuses: List<TimeMask.VisibilityStatus>) {
        val iterator = decoders.entries.iterator()
        while (iterator.hasNext()) {
            val (tag, decoder) = iterator.next()
            val visible = statuses.find { it.tag == tag }?.visible ?: false

            when {
                visible -> decoder.advance(ptsUsec)
                visible.not() && decoder.isUsed -> {
                    decoder.release()
                    iterator.remove()
                }
            }
        }
    }

    fun release() {
        decoders.values.forEach { it.release() }
    }
}