package com.netherpyro.glcv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlLayoutHelper.Companion.NO_MARGIN
import com.netherpyro.glcv.touches.GlTouchHelper
import com.netherpyro.glcv.util.AspectRatioChooser
import com.netherpyro.glcv.util.EConfigChooser
import com.netherpyro.glcv.util.GlAspectRatio
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

    var enableGestures = false

    private val renderer: GlRenderer
    private val layoutHelper: GlLayoutHelper

    private val touchHelper: GlTouchHelper
    @ColorInt
    private val defaultBaseColor: Int = Color.parseColor("#5555ff")
    @ColorInt
    private val defaultViewportColor: Int = Color.parseColor("#ff5555")

    private val defaultViewportAspectRatio = 1f
    private var aspectRatioChooser: AspectRatioChooser? = null

    private var viewportSizeChangedListener: ((Size) -> Unit)? = null
    private var initialAspectRatioListener: ((GlAspectRatio) -> Unit)? = null

    init {
        setEGLContextFactory(this)
        setEGLConfigChooser(EConfigChooser())
        holder.setFormat(PixelFormat.RGBA_8888)

        renderer = GlRenderer(this, defaultBaseColor, defaultViewportColor)
        layoutHelper = GlLayoutHelper(defaultViewportAspectRatio)
        touchHelper = GlTouchHelper(context, renderer)

        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun requestDraw() {
        queueEvent { requestRender() }
    }

    override fun postAction(action: Runnable) {
        queueEvent(action)
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

        if (!egl.eglDestroyContext(display, context)) {
            throw RuntimeException("eglDestroyContext" + egl.eglGetError())
        }
    }

    override fun onLayerAspectRatio(aspectValue: Float) {
        val glAspectRatio = aspectRatioChooser?.selectNearestAspect(aspectValue) ?: GlAspectRatio("", aspectValue)
        initialAspectRatioListener?.invoke(glAspectRatio)

        setAspectRatioInternal(
                aspectValue = glAspectRatio.value,
                animated = false
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (enableGestures) {
            return touchHelper.onTouchEvent(event)
        } else super.onTouchEvent(event)
    }

    fun addVideoLayer(tag: String? = null, onSurfaceAvailable: (Surface) -> Unit, applyLayerAspect: Boolean = false): VideoTransformable {
        return renderer.addVideoLayer(tag, onSurfaceAvailable, applyLayerAspect)
    }

    fun addImageLayer(tag: String? = null, bitmap: Bitmap, applyLayerAspect: Boolean = false): Transformable {
        return renderer.addImageLayer(tag, bitmap, applyLayerAspect)
    }

    fun bringToFront(transformable: Transformable) {
        renderer.bringLayerToFront(transformable)
    }

    fun bringToPosition(position: Int, transformable: Transformable) {
        renderer.bringLayerToPosition(position, transformable)
    }

    fun remove(transformable: Transformable) {
        renderer.removeLayer(transformable)
    }

    fun restoreOrder() {
        renderer.restoreLayersOrder()
    }

    fun listenViewportSizeChanged(listener: (Size) -> Unit) {
        viewportSizeChangedListener = listener
    }

    /**
     * Sets preferred aspects, one of them being apply automatically once one of layers has given its aspect
     *
     * Make sure this function called before layers being added for proper initial aspect installation
     * */
    fun setAspectsPreset(aspectRatioPresetList: List<GlAspectRatio>,
                         initialAspectRatioListener: ((GlAspectRatio) -> Unit)? = null) {
        this.aspectRatioChooser = AspectRatioChooser(aspectRatioPresetList)
        this.initialAspectRatioListener = initialAspectRatioListener
    }

    fun setAspectRatio(aspect: Float, animated: Boolean = false) {
        setAspectRatioInternal(aspect, animated)
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

    fun setViewportMargin(
            left: Int = NO_MARGIN,
            top: Int = NO_MARGIN,
            right: Int = NO_MARGIN,
            bottom: Int = NO_MARGIN
    ) {
        val viewport = layoutHelper.setViewportMargin(left, top, right, bottom)
        updateViewport(viewport)
    }

    private fun setAspectRatioInternal(aspectValue: Float, animated: Boolean) {
        layoutHelper.changeAspectRatio(aspectValue, animated) { viewport ->
            updateViewport(viewport)
        }
    }

    private fun updateViewport(vp: GlViewport) {
        renderer.setViewport(vp)
        touchHelper.viewportSize = vp.toSizeF()

        viewportSizeChangedListener?.also { post { it.invoke(vp.toSize()) } }

        requestDraw()
    }
}