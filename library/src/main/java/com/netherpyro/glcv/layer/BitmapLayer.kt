package com.netherpyro.glcv.layer

import android.graphics.Bitmap
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.shader.GlImageShader

/**
 * @author mmikhailov on 2019-12-04.
 */
internal class BitmapLayer(
        id: Int,
        tag: String?,
        position: Int,
        invalidator: Invalidator,
        private val bitmap: Bitmap
): Layer(id, tag, position, invalidator) {

    override val shader = GlImageShader(bitmap)

    override fun onSetup() {
        release()

        aspect = bitmap.width / bitmap.height.toFloat()
        shader.setup()

        invalidator.invalidate()
    }

    override fun onDrawFrame() {
        shader.draw(mvpMatrix, aspect)
    }

    override fun onRelease() {
        shader.release()
    }
}