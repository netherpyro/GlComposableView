package com.netherpyro.glcv

import kotlin.math.min

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class GlLayoutHelper(private var viewportAspect: Float) {

    companion object {
        const val NO_PADDING = -1
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var viewportPaddingLeft = 0
    private var viewportPaddingTop = 0
    private var viewportPaddingRight = 0
    private var viewportPaddingBottom = 0
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

    fun setViewportPadding(left: Int, top: Int, right: Int, bottom: Int): GlViewport {
        viewportPaddingLeft = if (left == NO_PADDING) viewportPaddingLeft else left
        viewportPaddingTop = if (top == NO_PADDING) viewportPaddingTop else top
        viewportPaddingRight = if (right == NO_PADDING) viewportPaddingRight else right
        viewportPaddingBottom = if (bottom == NO_PADDING) viewportPaddingBottom else bottom

        return recalculateViewport()
    }

    private fun recalculateViewport(): GlViewport {
        val x: Int
        val y: Int
        val h: Int
        val w: Int

        val maxW = viewWidth - viewportPaddingLeft - viewportPaddingRight
        val maxH = viewHeight - viewportPaddingTop - viewportPaddingBottom

        if (viewportAspect >= 1f) {
            h = min((maxW / viewportAspect).toInt(), maxH)
            w = (h * viewportAspect).toInt() + 1
        } else {
            w = min((maxH * viewportAspect).toInt(), maxW)
            h = (w / viewportAspect).toInt() + 1
        }

        x = ((maxW - w) / 2f).toInt() + viewportPaddingLeft
        y = ((maxH - h) / 2f).toInt() + viewportPaddingBottom

        viewport = GlViewport(x, y, w, h)

        return viewport
    }
}