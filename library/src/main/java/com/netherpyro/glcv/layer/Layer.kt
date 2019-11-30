package com.netherpyro.glcv.layer

import com.netherpyro.glcv.Invalidator

/**
 * @author mmikhailov on 2019-11-30.
 */
abstract class Layer(protected val invalidator: Invalidator) {

    abstract fun onGlPrepared()
    abstract fun onDrawFrame()
    abstract fun onViewportAspectRatioChanged(aspect: Float)
    abstract fun setScale(scaleFactor: Float)
    abstract fun setRotation(rotation: Int)
    abstract fun setTranslation(x: Int, y: Int)
    abstract fun release()
}