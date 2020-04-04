package com.netherpyro.glcv.baker.decode

import android.content.Context
import android.net.Uri

/**
 * @author mmikhailov on 04.04.2020.
 */
internal class DecoderPool {

    private val decoders = mutableMapOf<String, MoviePassiveDecoder>()

    fun createDecoderForTag(tag: String, context: Context, uri: Uri): MoviePassiveDecoder {
        val decoder = MoviePassiveDecoder(context, uri)
        decoders[tag] = decoder

        return decoder
    }

    fun advance() {
        decoders.values.forEach { it.advance() }
    }
}