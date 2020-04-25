package com.netherpyro.glcv

import android.util.Size

/**
 * @author mmikhailov on 25.04.2020.
 */
data class TransformData(
        val scale: Float = 1f,
        val rotation: Float = 0f,
        val xFactor: Float = 0f,
        val yFactor: Float = 0f,
        val opacity: Float = 1f,
        val skipDraw: Boolean = false,
        val layerSize: Size? = null
)