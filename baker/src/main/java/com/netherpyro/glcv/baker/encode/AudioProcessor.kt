package com.netherpyro.glcv.baker.encode

import java.nio.ByteBuffer

/**
 * @author mmikhailov on 28.04.2020.
 */
internal class AudioProcessor : AudioBufferProvider {

    private val buffers = mutableSetOf<AudioBuffer>()

    override fun provide(tag: String) = AudioBuffer(tag).also { buffers.add(it) }

    fun processData(): EncoderInput? {
        var result: EncoderInput? = null

        val iterator = buffers.iterator()
        while (iterator.hasNext()) {
            val audioBuffer = iterator.next()
            if (audioBuffer.data != null) {
                // todo merge audio with other audio buffers
                result = EncoderInput(audioBuffer.data!!, audioBuffer.size!!)
            }
        }

        return result
    }

    data class EncoderInput(
            val byteBuffer: ByteBuffer,
            val size: Int
    )
}