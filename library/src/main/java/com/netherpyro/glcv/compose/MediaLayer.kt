package com.netherpyro.glcv.compose

import com.netherpyro.glcv.Transformable

/**
 * @author mmikhailov on 03.04.2020.
 */
class MediaLayer(
        val tag: String,
        val srcPath: String,
        val transformable: Transformable
) {

    var isVisible: Boolean = true
        set(value) {
            field = value
            transformable.setSkipDraw(!value)
        }
}