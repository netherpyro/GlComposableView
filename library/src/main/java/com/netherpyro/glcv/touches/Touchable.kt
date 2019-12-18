package com.netherpyro.glcv.touches

import com.netherpyro.glcv.Transformable

/**
 * @author mmikhailov on 2019-12-18.
 */
internal data class Touchable(
        var scaleFactor: Float = 1f,
        var rotationAngle: Float = 0f,
        val transformable: Transformable
)