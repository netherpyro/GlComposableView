package com.netherpyro.glcv.baker

import com.netherpyro.glcv.RenderHost

/**
 * @author mmikhailov on 01.04.2020.
 */
object RenderHostStub : RenderHost {
    override fun requestRender() {
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
    }

    override fun enqueueEvent(runnable: Runnable) {
    }
}