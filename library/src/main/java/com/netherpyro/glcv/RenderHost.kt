package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-11-30.
 */
interface RenderHost {
    fun requestRender()
    fun onSurfaceChanged(width: Int, height: Int)
    fun enqueueEvent(runnable: Runnable)
}