package com.netherpyro.glcv

import kotlin.math.min

/**
 * @author mmikhailov on 2019-11-30.
 */
class GlLayoutHelper(private var viewportAspect: Float) {

    private var viewportW = 0
    private var viewportH = 0
    private var viewWidth = 0
    private var viewHeight = 0
    private var viewport = GlViewport()

    fun onSurfaceChanged(width: Int, height: Int): GlViewport {
        viewWidth = width
        viewHeight = height

        return recalculateViewport()
    }

    fun changeAspectRatio(aspect: Float): GlViewport {
        viewportAspect = aspect

        return recalculateViewport()
    }

    private fun recalculateViewport(): GlViewport {
        if (viewportAspect >= 1f) {
            viewportH = min((viewWidth / viewportAspect).toInt(), viewHeight)
            viewportW = (viewportH * viewportAspect).toInt() + 1
        } else {
            viewportW = min((viewHeight * viewportAspect).toInt(), viewWidth)
            viewportH = (viewportW / viewportAspect).toInt() + 1
        }

        viewport = GlViewport(0, 0, viewportW, viewportH)

        return viewport
    }
}