package com.netherpyro.glcv

import kotlin.math.min

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class GlLayoutHelper(var viewportAspect: Float) {

    companion object {
        const val NO_MARGIN = -1
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var viewportMarginLeft = 0
    private var viewportMarginTop = 0
    private var viewportMarginRight = 0
    private var viewportMarginBottom = 0
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

    fun setViewportMargin(left: Int, top: Int, right: Int, bottom: Int): GlViewport {
        viewportMarginLeft = if (left == NO_MARGIN) viewportMarginLeft else left
        viewportMarginTop = if (top == NO_MARGIN) viewportMarginTop else top
        viewportMarginRight = if (right == NO_MARGIN) viewportMarginRight else right
        viewportMarginBottom = if (bottom == NO_MARGIN) viewportMarginBottom else bottom

        return recalculateViewport()
    }

    private fun recalculateViewport(): GlViewport {
        val x: Int
        val y: Int
        val h: Int
        val w: Int

        val maxW = viewWidth - viewportMarginLeft - viewportMarginRight
        val maxH = viewHeight - viewportMarginTop - viewportMarginBottom

        if (viewportAspect >= 1f) {
            h = min((maxW / viewportAspect).toInt(), maxH)
            w = (h * viewportAspect).toInt() + 1
        } else {
            w = min((maxH * viewportAspect).toInt(), maxW)
            h = (w / viewportAspect).toInt() + 1
        }

        x = ((maxW - w) / 2f).toInt() + viewportMarginLeft
        y = ((maxH - h) / 2f).toInt() + viewportMarginBottom

        viewport = GlViewport(x, y, w, h)

        return viewport
    }
}