package com.netherpyro.glcv.util

import android.opengl.GLSurfaceView.EGLContextFactory
import timber.log.Timber
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author mmikhailov on 2019-11-30.
 */
class EContextFactory : EGLContextFactory {

    override fun createContext(egl: EGL10, display: EGLDisplay,
                               config: EGLConfig): EGLContext {
        val attribList: IntArray = intArrayOf(0x3098, 2, EGL10.EGL_NONE)

        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList)
    }

    override fun destroyContext(egl: EGL10, display: EGLDisplay,
                                context: EGLContext) {
        if (!egl.eglDestroyContext(display, context)) {
            Timber.e("display:$display context: $context")
            throw RuntimeException("eglDestroyContex" + egl.eglGetError())
        }
    }
}