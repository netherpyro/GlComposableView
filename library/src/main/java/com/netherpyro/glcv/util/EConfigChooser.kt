package com.netherpyro.glcv.util

import android.opengl.EGL14
import android.opengl.GLSurfaceView.EGLConfigChooser
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author mmikhailov on 2019-11-30.
 */
class EConfigChooser : EGLConfigChooser {

    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 2
    }

    private val configSpec: IntArray
    private val redSize: Int = 8
    private val greenSize: Int = 8
    private val blueSize: Int = 8
    private val alphaSize: Int = 8
    private val depthSize: Int = 8
    private val stencilSize: Int = 0

    init {
        configSpec = filterConfigSpec(intArrayOf(
                EGL10.EGL_RED_SIZE, redSize,
                EGL10.EGL_GREEN_SIZE, greenSize,
                EGL10.EGL_BLUE_SIZE, blueSize,
                EGL10.EGL_ALPHA_SIZE, alphaSize,
                EGL10.EGL_DEPTH_SIZE, depthSize,
                EGL10.EGL_STENCIL_SIZE, stencilSize,
                EGL10.EGL_NONE
        ), EGL_CONTEXT_CLIENT_VERSION)
    }

    private fun filterConfigSpec(configSpec: IntArray, version: Int): IntArray {
        if (version != 2) {
            return configSpec
        }

        val len = configSpec.size
        val newConfigSpec = IntArray(len + 2)
        System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
        newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
        newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT
        newConfigSpec[len + 1] = EGL10.EGL_NONE

        return newConfigSpec
    }

    override fun chooseConfig(egl: EGL10,
                              display: EGLDisplay): EGLConfig {
        val numConfig = IntArray(1)
        require(egl.eglChooseConfig(display, configSpec, null, 0, numConfig)) { "eglChooseConfig failed" }

        val configSize = numConfig[0]
        require(configSize > 0) { "No configs match configSpec" }

        val configs = arrayOfNulls<EGLConfig>(configSize)
        require(egl.eglChooseConfig(display, configSpec, configs, configSize, numConfig)) { "eglChooseConfig#2 failed" }

        return chooseConfig(egl, display, configs) ?: throw IllegalArgumentException("No config chosen")
    }

    private fun chooseConfig(egl: EGL10, display: EGLDisplay,
                             configs: Array<EGLConfig?>): EGLConfig? {

        for (config in configs) {
            val d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0)
            val s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0)

            if (d >= depthSize && s >= stencilSize) {
                val r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0)
                val g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0)
                val b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0)
                val a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0)

                if (r == redSize && g == greenSize && b == blueSize && a == alphaSize) {
                    return config
                }
            }
        }

        return null
    }

    private fun findConfigAttrib(
            egl: EGL10,
            display: EGLDisplay,
            config: EGLConfig?,
            attribute: Int,
            defaultValue: Int
    ): Int {
        val value = IntArray(1)

        return if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
            value[0]
        } else defaultValue
    }
}