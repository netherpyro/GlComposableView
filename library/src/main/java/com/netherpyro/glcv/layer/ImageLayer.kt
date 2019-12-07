package com.netherpyro.glcv.layer

import android.graphics.Bitmap
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.shader.GlImageShader

/**
 * @author mmikhailov on 2019-12-04.
 */
internal class ImageLayer(
        id: Int,
        invalidator: Invalidator,
        bitmap: Bitmap
): Layer(id, invalidator) {

    override val shader = GlImageShader(bitmap)

    init {
        aspect = bitmap.width / bitmap.height.toFloat()
    }

    override fun onGlPrepared() {
        shader.setup()
    }

    override fun onDrawFrame() {
        shader.draw(mvpMatrix, aspect)
    }

    override fun release() {
        shader.release()
    }
}