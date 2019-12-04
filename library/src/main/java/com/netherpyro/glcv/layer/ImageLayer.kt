package com.netherpyro.glcv.layer

import android.graphics.Bitmap
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.shader.GlImageShader
import com.netherpyro.glcv.shader.GlShader

/**
 * @author mmikhailov on 2019-12-04.
 */
internal class ImageLayer(
        private val bitmap: Bitmap,
        invalidator: Invalidator
) : Layer(invalidator) {

    override lateinit var shader: GlShader

    init {
        aspect = bitmap.width / bitmap.height.toFloat()
    }

    override fun onGlPrepared() {
        shader = GlImageShader(bitmap)
        shader.setup()
    }

    override fun onDrawFrame() {
        (shader as GlImageShader).draw(mvpMatrix, aspect)
    }

    override fun release() {
        shader.release()
    }
}