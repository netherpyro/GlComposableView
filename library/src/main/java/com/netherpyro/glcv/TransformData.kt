package com.netherpyro.glcv

import android.util.Size
import com.netherpyro.glcv.compose.template.TemplateUnit

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

fun TemplateUnit.asTransformData(width: Int? = null, height: Int? = null) = TransformData(
        scale = scaleFactor,
        rotation = rotationDeg,
        xFactor = -translateFactorX,
        yFactor = translateFactorY,
        opacity = opacity,
        layerSize = if (width != null && height != null) Size(width, height) else null
)