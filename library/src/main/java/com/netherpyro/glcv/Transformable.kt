package com.netherpyro.glcv

import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.Px

/**
 * @author mmikhailov on 2019-12-04.
 */
interface Transformable {

    val id: Int
    val tag: String?

    var enableGesturesTransform: Boolean

    fun setLayerPosition(position: Int)

    fun setSkipDraw(skip: Boolean)

    fun setScale(scaleFactor: Float)
    fun setRotation(rotationDeg: Float)
    fun setTranslation(@Px x: Float, @Px y: Float) // todo revisit pixel translation
    fun setTranslationFactor(xFactor: Float, yFactor: Float)
    fun setOpacity(opacity: Float)
    fun setBorder(width: Float, @ColorInt color: Int)
    fun setSize(width: Float, height: Float)

    fun getRotation(): Float
    fun getScale(): Float
    fun getTranslation(): Pair<Float, Float>
    fun getTranslationFactor(): Pair<Float, Float>
    fun getFrustumRect(): RectF
    fun getLayerAspect(): Float
    fun getLayerPosition(): Int
}