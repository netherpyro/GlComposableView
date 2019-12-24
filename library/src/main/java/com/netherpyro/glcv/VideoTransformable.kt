package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-12-04.
 */
interface VideoTransformable : Transformable {

    fun setVideoSize(width: Float, height: Float)
    fun setDrawLastFrameIfNoFramesAvailable(draw: Boolean)
}