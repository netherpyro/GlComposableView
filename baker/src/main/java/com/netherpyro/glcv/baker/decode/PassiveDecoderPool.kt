package com.netherpyro.glcv.baker.decode

import android.content.Context
import android.net.Uri
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.compose.template.TimeMask

/**
 * @author mmikhailov on 04.04.2020.
 */
internal class PassiveDecoderPool {

    private val decoders = mutableMapOf<String, MoviePassiveDecoder>()

    fun createSurfaceConsumer(tag: String, context: Context, uri: Uri): SurfaceConsumer {
        val decoder = MoviePassiveDecoder(context, uri)
        decoders[tag] = decoder

        return decoder
    }

    fun prepare() {
        decoders.values.forEach { it.raiseDecoder() }
    }

    fun advance(statuses: List<TimeMask.VisibilityStatus>) {
        val iterator = decoders.entries.iterator()
        while (iterator.hasNext()) {
            val (tag, decoder) = iterator.next()
            val visible = statuses.find { it.tag == tag }?.visible ?: false

            when {
                visible -> decoder.advance()
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