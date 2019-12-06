package com.netherpyro.glcv.util

import kotlin.math.abs

/**
 * @author mmikhailov on 2019-12-06.
 */
internal class AspectRatioChooser(private vararg val presetAspects: Float) {

    fun selectNearestAspect(aspect: Float): Float? =
            presetAspects
                .map { it to abs(aspect - it) }
                .minBy { it.second }
                ?.first
}