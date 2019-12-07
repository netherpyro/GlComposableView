package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-12-04.
 */
interface Transformable {

    val id: Int

    fun setRotation(rotationDeg: Float)
    fun setScale(scaleFactor: Float)
    fun setTranslation(x: Float, y: Float)
}