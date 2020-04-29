package com.netherpyro.glcv.baker.encode

import java.nio.ByteBuffer

/**
 * @author mmikhailov on 28.04.2020.
 */
internal class AudioBuffer(val tag: String) {

    var data: ByteBuffer? = null
        private set
    var size: Int? = null
        private set

    fun update(byteBuffer: ByteBuffer?, size: Int) {
        this.data = byteBuffer
        this.size = if (byteBuffer != null) size else null
    }
}