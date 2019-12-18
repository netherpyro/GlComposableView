package com.netherpyro.glcv

import androidx.annotation.ColorInt

/**
 * @author mmikhailov on 2019-12-04.
 */
interface Transformable {

    val id: Int
    val tag: String?

    fun setRotation(rotationDeg: Float)
    fun setScale(scaleFactor: Float)
    fun setTranslation(x: Float, y: Float)
    fun setOpacity(opacity: Float)
    fun setBorder(width: Float, @ColorInt color: Int)
}