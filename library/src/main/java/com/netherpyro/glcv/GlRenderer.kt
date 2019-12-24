package com.netherpyro.glcv

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glScissor
import android.opengl.GLES20.glViewport
import android.view.Surface
import androidx.annotation.ColorInt
import com.netherpyro.glcv.extensions.alpha
import com.netherpyro.glcv.extensions.blue
import com.netherpyro.glcv.extensions.green
import com.netherpyro.glcv.extensions.red
import com.netherpyro.glcv.layer.ImageLayer
import com.netherpyro.glcv.layer.Layer
import com.netherpyro.glcv.layer.VideoLayer

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class GlRenderer(
        private val renderHost: RenderHost,
        @ColorInt
        var backgroundColor: Int,
        @ColorInt
        var viewportColor: Int
) : FrameBufferObjectRenderer(), Invalidator, Observable {

    private val layers = mutableListOf<Layer>()

    private var viewport = GlViewport()

    private var addLayerAction: ((Transformable) -> Unit)? = null
    private var removeLayerAction: ((Int) -> Unit)? = null

    private var surfaceReady = false
    private var nextId = 0

    @Synchronized
    override fun onSurfaceCreated() {
        glClearColor(backgroundColor.red(), backgroundColor.green(), backgroundColor.blue(), backgroundColor.alpha())
        glEnable(GLES20.GL_BLEND)
        glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        surfaceReady = true

        layers.forEach { it.setup() }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        renderHost.onSurfaceChanged(width, height)
    }

    @Synchronized
    override fun onDrawFrame(fbo: FramebufferObject) {
        glClearColor(backgroundColor.red(), backgroundColor.green(), backgroundColor.blue(), backgroundColor.alpha())
        glClear(GLES20.GL_COLOR_BUFFER_BIT)

        glViewport(viewport.x, viewport.y, viewport.width, viewport.height)

        glScissor(viewport.x, viewport.y, viewport.width, viewport.height)
        glEnable(GLES20.GL_SCISSOR_TEST)

        glClearColor(viewportColor.red(), viewportColor.green(), viewportColor.blue(), viewportColor.alpha())
        glClear(GLES20.GL_COLOR_BUFFER_BIT)

        glDisable(GLES20.GL_SCISSOR_TEST)

        layers.forEach { it.draw() }
    }

    @Synchronized
    override fun onRelease() {
        surfaceReady = false

        layers.forEach { it.release() }
    }

    override fun invalidate() {
        renderHost.requestDraw()
    }

    override fun subscribeLayersChange(addAction: (Transformable) -> Unit, removeAction: (Int) -> Unit): List<Transformable> {
        this.addLayerAction = addAction
        this.removeLayerAction = removeAction

        return layers.toList()
    }

    @Synchronized
    fun setViewport(viewport: GlViewport) {
        this.viewport = viewport
        this.layers.forEach { it.onViewportUpdated(viewport) }
    }

    fun addVideoLayer(tag: String?, onSurfaceAvailable: (Surface) -> Unit, applyLayerAspect: Boolean): VideoTransformable {
        return VideoLayer(nextId++, tag, this, onSurfaceAvailable)
            .also { addLayer(it, applyLayerAspect) }
    }

    fun addImageLayer(tag: String?, bitmap: Bitmap, applyLayerAspect: Boolean): Transformable {
        return ImageLayer(nextId++, tag, this, bitmap)
            .also { addLayer(it, applyLayerAspect) }
    }

    fun bringLayerToFront(transformable: Transformable) {
        bringLayerToPosition(layers.lastIndex, transformable)
    }

    @Synchronized
    fun bringLayerToPosition(position: Int, transformable: Transformable) {
        if (position >= 0) {
            val index = layers.indexOfFirst { it.id == transformable.id }
            val layer = layers.removeAt(index)
            layers.add(position, layer)

            invalidate()
        }
    }

    @Synchronized
    fun restoreLayersOrder() {
        layers.sortBy { it.id }

        invalidate()
    }

    @Synchronized
    fun removeLayer(transformable: Transformable) {
        with(layers) {
            removeAt(indexOfFirst { it.id == transformable.id }).release()
            removeLayerAction?.invoke(transformable.id)
        }

        invalidate()
    }

    @Synchronized
    private fun addLayer(layer: Layer, applyLayerAspect: Boolean) {
        if (applyLayerAspect) layer.listenAspectRatioReady { renderHost.onLayerAspectRatio(it) }

        if (surfaceReady) {
            renderHost.postAction(Runnable {
                layer.setup()
                layer.onViewportUpdated(viewport)
                layers.add(layer)
                addLayerAction?.invoke(layer)

                invalidate()
            })
        } else {
            layers.add(layer)
            addLayerAction?.invoke(layer)
        }
    }
}