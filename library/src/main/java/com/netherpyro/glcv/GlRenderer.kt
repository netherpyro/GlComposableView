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
import android.util.Log
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
) : FrameBufferObjectRenderer(), Invalidator, TransformableObservable {

    companion object {
        const val NO_POSITION = -1
    }

    private val layers = mutableListOf<Layer>()

    private var viewport = GlViewport()

    private var addLayerAction: ((Transformable) -> Unit)? = null
    private var removeLayerAction: ((Int) -> Unit)? = null
    private var changeLayerPositionsAction: (() -> Unit)? = null

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

    @Synchronized
    override fun claimPosition(layer: Layer, position: Int) {
        if (layers.size <= 1) return

        val lastAvailablePosition = layers.size - 1
        val finalPosition = position.coerceIn(0, lastAvailablePosition)

        if (layer.position == finalPosition) return

        layers.forEach {
            if (it.id != layer.id) {
                if (it.position > finalPosition) {
                    it.position = (it.position + 1).coerceAtMost(lastAvailablePosition)
                } else if (it.position <= finalPosition) {
                    it.position = (it.position - 1).coerceAtLeast(0)
                }
            }
        }

        layer.position = finalPosition
        layers.sortBy { it.position }

        changeLayerPositionsAction?.invoke()

        invalidate()
    }

    override fun subscribeLayersChange(
            addAction: (Transformable) -> Unit,
            removeAction: (Int) -> Unit,
            changeLayerPositionsAction: () -> Unit
    ): List<Transformable> {

        this.addLayerAction = addAction
        this.removeLayerAction = removeAction
        this.changeLayerPositionsAction = changeLayerPositionsAction

        return layers.toList()
    }

    @Synchronized
    fun setViewport(viewport: GlViewport) {
        this.viewport = viewport
        this.layers.forEach { it.onViewportUpdated(viewport) }
    }

    fun listLayers() {
        Log.d("GlRenderer", "listLayers::${layers.toTypedArray().contentToString()}\n\n")
    }

    fun addVideoLayer(tag: String?, onSurfaceAvailable: (Surface) -> Unit, applyLayerAspect: Boolean,
                      position: Int, onFrameAvailable: (() -> Unit)?): VideoTransformable {
        return VideoLayer(nextId++, tag, position, this, onSurfaceAvailable, onFrameAvailable)
            .also { addLayer(it, applyLayerAspect) }
    }

    fun addImageLayer(tag: String?, bitmap: Bitmap, applyLayerAspect: Boolean, position: Int): Transformable {
        return ImageLayer(nextId++, tag, position, this, bitmap)
            .also { addLayer(it, applyLayerAspect) }
    }

    @Synchronized
    fun removeLayer(transformable: Transformable) {
        var removedPosition: Int
        val removedIdx = layers.indexOfFirst { it.id == transformable.id }

        layers.removeAt(removedIdx)
            .also { removedLayer ->
                removedPosition = removedLayer.position
                removedLayer.release()
            }

        layers.forEach {
            if (it.position > removedPosition) {
                it.position -= 1
            }
        }

        layers.sortBy { it.position }

        removeLayerAction?.invoke(transformable.id)

        invalidate()
    }

    @Synchronized
    private fun addLayer(layer: Layer, applyLayerAspect: Boolean) {
        if (applyLayerAspect) layer.listenAspectRatioReady { renderHost.onLayerAspectRatio(it) }

        if (surfaceReady) {
            renderHost.postAction(Runnable {
                layer.setup()
                layer.onViewportUpdated(viewport)

                addLayerToList(layer)
                addLayerAction?.invoke(layer)

                invalidate()
            })
        } else {
            addLayerToList(layer)
            addLayerAction?.invoke(layer)
        }
    }

    private fun addLayerToList(layer: Layer) {
        if (layer.position < 0) {
            layer.position = layers.size
        } else {
            layers.forEach {
                if (it.position >= layer.position) {
                    it.position += 1
                }
            }
        }

        layers.add(layer)
        layers.sortBy { it.position }
    }
}