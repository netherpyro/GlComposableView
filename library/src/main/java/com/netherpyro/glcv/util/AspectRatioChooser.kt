package com.netherpyro.glcv.util

import kotlin.math.abs

/**
 * @author mmikhailov on 2019-12-06.
 */
internal class AspectRatioChooser(private val presetAspects: List<GlAspectRatio>) {

    fun selectNearestAspect(aspect: Float): GlAspectRatio? =
            presetAspects
                .map { it to abs(aspect - it.value) }
                .minBy { it.second }
                ?.first
}