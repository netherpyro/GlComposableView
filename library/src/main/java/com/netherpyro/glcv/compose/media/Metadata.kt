package com.netherpyro.glcv.compose.media

/**
 * @author mmikhailov on 05.04.2020.
 */
data class Metadata(
        val type: Type,
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val orientation: Orientation
)