package com.netherpyro.glcv

import android.util.Size
import android.util.SizeF

internal data class GlViewport(
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0
)

internal fun GlViewport.toSize() = Size(width, height)
internal fun GlViewport.toSizeF() = SizeF(width.toFloat(), height.toFloat())
