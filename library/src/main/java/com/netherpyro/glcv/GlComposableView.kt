package com.netherpyro.glcv

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import com.netherpyro.glcv.LayoutHelper.Companion.NO_MARGIN
import com.netherpyro.glcv.touches.GlTouchHelper
import com.netherpyro.glcv.touches.LayerTouchListener
import com.netherpyro.glcv.util.EConfigChooser
import java.util.LinkedList
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import kotlin.math.abs

/**
 * @author mmikhailov on 2019-10-26.
 *
 */
// todo add option to (remove all / replace all with) layers
class GlComposableView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), RenderHost, GLSurfaceView.EGLContextFactory {

    var enableGestures = false

    val aspectRatio: Float
        get() = layoutHelper.viewportAspect

    private val renderer: GlRenderer
    private val layoutHelper: LayoutHelper
    private val touchHelper: GlTouchHelper

    private val eventQueue = LinkedList<Runnable>()

    @ColorInt
    private val defaultBaseColor: Int = Color.WHITE

    @ColorInt
    private val defaultViewportColor: Int = Color.BLACK

    private val defaultViewportAspectRatio = 1f

    private var changeAspectRatioAnimDuration = 150L

    private var viewReady = false

    private var viewportSizeChangedListener: ((Size) -> Unit)? = null

    init {
        setEGLContextFactory(this)
        setEGLConfigChooser(EConfigChooser())
        holder.setFormat(PixelFormat.RGBA_8888)

        renderer = GlRenderer(this, defaultBaseColor, defaultViewportColor)
        layoutHelper = LayoutHelper(defaultViewportAspectRatio)
        touchHelper = GlTouchHelper(context, renderer)

        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        post { holder.setFixedSize(width, height) }

        val viewport = layoutHelper.onSurfaceChanged(width, height)
        touchHelper.viewHeight = height
        updateViewport(viewport)

        while (eventQueue.isNotEmpty()) {
            eventQueue.removeFirst()
                .run()
        }

        viewReady = true
    }

    override fun createContext(egl: EGL10, display: EGLDisplay, config: EGLConfig): EGLContext {
        val attribList: IntArray = intArrayOf(0x3098, 2, EGL10.EGL_NONE)

        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList)
    }

    override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
        viewReady = false
        renderer.release()

        if (!egl.eglDestroyContext(display, context)) {
            throw RuntimeException("eglDestroyContext" + egl.eglGetError())
        }
    }

    override fun enqueueEvent(runnable: Runnable) {
        if (viewReady) {
            queueEvent(runnable)
        } else {
            eventQueue.addLast(runnable)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (enableGestures) {
            return touchHelper.onTouchEvent(event)
        } else super.onTouchEvent(event)
    }

    fun listLayers() = renderer.listLayers()

    fun addSurfaceLayer(
            tag: String? = null,
            surfaceConsumer: SurfaceConsumer,
            position: Int = GlRenderer.TOP_POSITION,
            initialValues: TransformData? = null,
            onFrameAvailable: ((Long) -> Unit)? = null,
            onTransformable: (Transformable) -> Unit
    ) = enqueueEvent {
        val t: Transformable = renderer.addSurfaceLayer(
                tag,
                surfaceConsumer,
                position,
                initialValues,
                onFrameAvailable
        )
        onTransformable(t)
    }

    fun addBitmapLayer(
            tag: String? = null,
            bitmap: Bitmap,
            position: Int = GlRenderer.TOP_POSITION,
            initialValues: TransformData? = null,
            onTransformable: (Transformable) -> Unit
    ) = enqueueEvent {
        val t: Transformable = renderer.addBitmapLayer(tag, bitmap, position, initialValues)
        onTransformable(t)
    }

    fun removeLayer(transformable: Transformable) = enqueueEvent {
        renderer.removeLayer(transformable)
    }

    fun listenViewportSizeChanged(listener: (Size) -> Unit) {
        viewportSizeChangedListener = listener
    }

    fun listenTouches(layerTouchListener: LayerTouchListener) {
        touchHelper.touchesListener = layerTouchListener
    }

    fun setAspectRatio(aspect: Float, animated: Boolean = false) = enqueueEvent {
        setAspectRatioInternal(aspect, animated)
    }

    fun setBaseColor(@ColorInt color: Int) = enqueueEvent {
        renderer.backgroundColor = color
        requestRender()
    }

    fun setViewportColor(@ColorInt color: Int) = enqueueEvent {
        renderer.viewportColor = color
        requestRender()
    }

    fun setViewportMargin(
            left: Int = NO_MARGIN,
            top: Int = NO_MARGIN,
            right: Int = NO_MARGIN,
            bottom: Int = NO_MARGIN
    ) = enqueueEvent {
        val viewport = layoutHelper.setViewportMargin(left, top, right, bottom)
        updateViewport(viewport)
    }

    fun setMaxScale(factor: Float) {
        touchHelper.maxScale = factor
    }

    fun setMinScale(factor: Float) {
        touchHelper.minScale = factor
    }

    fun enableOnClickLayerIteration(enable: Boolean) {
        touchHelper.useIteration = enable
    }

    private fun setAspectRatioInternal(targetValue: Float, animated: Boolean) {
        val currentValue = layoutHelper.viewportAspect

        if (!animated || abs(currentValue - targetValue) > 1f) {
            updateViewport(layoutHelper.changeAspectRatio(targetValue))
        } else {
            ValueAnimator.ofFloat(currentValue, targetValue)
                .apply {
                    duration = changeAspectRatioAnimDuration
                    interpolator = AccelerateInterpolator()
                    addUpdateListener {
                        updateViewport(layoutHelper.changeAspectRatio(it.animatedValue as Float))
                    }
                    doOnEnd { updateViewport(layoutHelper.changeAspectRatio(targetValue)) }
                    post { start() }
                }
        }
    }

    private fun updateViewport(vp: GlViewport) {
        renderer.setViewport(vp)
        touchHelper.viewport = vp

        viewportSizeChangedListener?.also { listener -> post { listener.invoke(vp.toSize()) } }

        requestRender()
    }

    private inline fun enqueueEvent(crossinline r: () -> Unit) {
        if (viewReady) {
            queueEvent { r.invoke() }
        } else {
            eventQueue.addLast(Runnable { r.invoke() })
        }
    }
}