package com.netherpyro.glcv

import android.opengl.GLES20
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glScissor
import android.opengl.GLES20.glViewport
import androidx.annotation.ColorInt
import com.netherpyro.glcv.extensions.alpha
import com.netherpyro.glcv.extensions.blue
import com.netherpyro.glcv.extensions.green
import com.netherpyro.glcv.extensions.red
import timber.log.Timber

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class GlRenderer(
        private val renderMediator: GlRenderMediator,
        @ColorInt
        var backgroundColor: Int,
        @ColorInt
        var viewportColor: Int
) : FrameBufferObjectRenderer() {

    private var viewport = GlViewport()

    override fun onSurfaceCreated() {
        glClearColor(backgroundColor.red(), backgroundColor.green(), backgroundColor.blue(), backgroundColor.alpha())

        renderMediator.onSurfaceCreated()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Timber.d("onSurfaceChanged width = $width  height = $height")

        renderMediator.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(fbo: FramebufferObject) {
        glClearColor(backgroundColor.red(), backgroundColor.green(), backgroundColor.blue(), backgroundColor.alpha())
        glClear(GLES20.GL_COLOR_BUFFER_BIT)

        glViewport(0, 0, viewport.width, viewport.height)

        glScissor(0, 0, viewport.width, viewport.height)
        glEnable(GLES20.GL_SCISSOR_TEST)

        glClearColor(viewportColor.red(), viewportColor.green(), viewportColor.blue(), viewportColor.alpha())
        glClear(GLES20.GL_COLOR_BUFFER_BIT)

        glDisable(GLES20.GL_SCISSOR_TEST)

        renderMediator.onDrawFrame(fbo)
    }

    fun setViewport(viewport: GlViewport) {
        this.viewport = viewport
    }

}