package com.netherpyro.glcv.compose

import com.netherpyro.glcv.GlRenderer

/**
 * @author mmikhailov on 28.03.2020.
 */
enum class ZOrderDirection {
    TOP, BOTTOM
}

fun ZOrderDirection.toGlRenderPosition(): Int = when(this) {
    ZOrderDirection.TOP -> GlRenderer.TOP_POSITION
    ZOrderDirection.BOTTOM -> GlRenderer.BOTTOM_POSITION
}