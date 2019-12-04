package com.netherpyro.glcv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.google.android.exoplayer2.SimpleExoPlayer
import com.netherpyro.glcv.GlLayoutHelper.Companion.NO_PADDING
import com.netherpyro.glcv.util.EConfigChooser
import timber.log.Timber
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author mmikhailov on 2019-10-26.
 *
 */
class GlComposableView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), RenderHost, GLSurfaceView.EGLContextFactory {

    private val renderer: GlRenderer
    private val renderMediator: GlRenderMediator
    private val layoutHelper: GlLayoutHelper

    @ColorInt
    private val defaultBaseColor: Int = Color.parseColor("#5555ff")
    @ColorInt
    private val defaultViewportColor: Int = Color.parseColor("#ff5555")
    private val defaultViewportAspectRatio = 1f

    init {
        setEGLContextFactory(this)
        setEGLConfigChooser(EConfigChooser())

        layoutHelper = GlLayoutHelper(defaultViewportAspectRatio)
        renderMediator = GlRenderMediator(this)
        renderer = GlRenderer(renderMediator, defaultBaseColor, defaultViewportColor)

        setRenderer(renderer)
    }

    override fun requestDraw() {
        queueEvent { requestRender() }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        post { holder.setFixedSize(width, height) }

        val viewport = layoutHelper.onSurfaceChanged(width, height)
        updateViewport(viewport)
    }

    override fun createContext(egl: EGL10, display: EGLDisplay,
                               config: EGLConfig): EGLContext {
        val attribList: IntArray = intArrayOf(0x3098, 2, EGL10.EGL_NONE)

        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList)
    }

    override fun destroyContext(egl: EGL10, display: EGLDisplay,
                                context: EGLContext) {
        renderer.release()
        renderMediator.release()

        if (!egl.eglDestroyContext(display, context)) {
            Timber.e("display:$display context: $context")
            throw RuntimeException("eglDestroyContex" + egl.eglGetError())
        }
    }

    fun addExoPlayerLayer(player: SimpleExoPlayer): Transformable {
        return renderMediator.addExoPlayerLayer(player)
    }

    fun addImageLayer(bitmap: Bitmap): Transformable {
        return renderMediator.addImageLayer(bitmap)
    }

    fun setAspectRatio(aspect: Float, animated: Boolean = false) {
        layoutHelper.changeAspectRatio(aspect, animated) {
            updateViewport(it)
        }
    }

    fun setBaseColor(@ColorInt color: Int) {
        renderer.backgroundColor = color

        requestDraw()
    }

    fun setViewportColor(@ColorInt color: Int) {
        renderer.viewportColor = color

        requestDraw()
    }

    fun setChangeAspectRatioAnimationDuration(duration: Long) {
        layoutHelper.animDuration = duration
    }

    fun setViewportPadding(
            left: Int = NO_PADDING,
            top: Int = NO_PADDING,
            right: Int = NO_PADDING,
            bottom: Int = NO_PADDING
    ) {
        val viewport = layoutHelper.setViewportPadding(left, top, right, bottom)
        updateViewport(viewport)
    }

    private fun updateViewport(vp: GlViewport) {
        renderMediator.onViewportChanged(vp)
        renderer.setViewport(vp)

        requestDraw()
    }
}