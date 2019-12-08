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

    private var initialized = false

    init {
        aspect = bitmap.width / bitmap.height.toFloat()
    }

    override fun setup() {
        release()

        shader.setup()

        initialized = true
        invalidator.invalidate()
    }

    override fun onDrawFrame() {
        if (initialized) shader.draw(mvpMatrix, aspect)
    }

    override fun release() {
        initialized = false
        shader.release()
    }
}