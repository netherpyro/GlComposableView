package com.netherpyro.glcv.layer

import android.graphics.Bitmap
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.TransformData
import com.netherpyro.glcv.shader.GlImageShader

/**
 * @author mmikhailov on 2019-12-04.
 */
internal class BitmapLayer(
        id: Int,
        tag: String?,
        position: Int,
        invalidator: Invalidator,
        initialValues: TransformData?,
        private val bitmap: Bitmap
): Layer(id, tag, position, invalidator, initialValues) {

    override val shader = GlImageShader(bitmap)

    init {
        shader.opacity = initialValues?.opacity ?: 1f
    }

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