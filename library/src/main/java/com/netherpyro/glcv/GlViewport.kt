package com.netherpyro.glcv

import android.util.Size

data class GlViewport(
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0
)

fun GlViewport.toSize() = Size(width, height)
