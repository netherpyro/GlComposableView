package com.netherpyro.glcv.baker.decode

import android.content.Context
import android.net.Uri
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.compose.template.TimeMask

/**
 * @author mmikhailov on 04.04.2020.
 */
// todo prepare decoder for manual looping
internal class PassiveDecoderPool {

    private val decoders = mutableMapOf<String, MoviePassiveDecoder>()

    fun createSurfaceConsumer(tag: String, context: Context, uri: Uri): SurfaceConsumer {
        val decoder = MoviePassiveDecoder(context, uri)
        decoders[tag] = decoder

        return decoder
    }

    fun advance(statuses: List<TimeMask.VisibilityStatus>) {
        decoders.forEach { (tag: String, decoder: MoviePassiveDecoder) ->

            decoder.advance()
        }
    }

    fun release() {
        // todo
    }
}