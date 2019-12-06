package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-11-30.
 */
internal interface RenderHost {
    fun requestDraw()
    fun onSurfaceChanged(width: Int, height: Int)
    fun onFirstLayerAspect(aspect: Float)
}