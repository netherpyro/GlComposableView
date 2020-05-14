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
import com.netherpyro.glcv.layer.BitmapLayer
import com.netherpyro.glcv.layer.Layer
import com.netherpyro.glcv.layer.SurfaceLayer

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
        const val TOP_POSITION = Int.MAX_VALUE
        const val BOTTOM_POSITION = Int.MIN_VALUE
    }

    private val layers = mutableListOf<Layer>()

    private var viewport = GlViewport()

    private var addLayerAction: ((Transformable) -> Unit)? = null
    private var removeLayerAction: ((Int) -> Unit)? = null
    private var changeLayerPositionsAction: (() -> Unit)? = null

    private var nextId = 0

    override fun onSurfaceCreated() {
        glDisable(GLES20.GL_DEPTH_TEST)
        glEnable(GLES20.GL_BLEND)
        glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        glClearColor(backgroundColor.red(), backgroundColor.green(), backgroundColor.blue(), backgroundColor.alpha())

        layers.forEach { it.setup() }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        renderHost.onSurfaceChanged(width, height)
    }

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

    override fun onRelease() {
        layers.forEach { it.release() }
    }

    override fun invalidate() {
        renderHost.requestRender()
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

    override fun claimLayerPosition(layer: Layer, position: Int) {
        renderHost.enqueueEvent(Runnable {
            moveLayer(layer, position)
        })
    }

    fun setViewport(viewport: GlViewport) {
        this.viewport = viewport
        this.layers.forEach { it.onViewportUpdated(viewport) }
    }

    fun listLayers(): String =
            layers.toTypedArray()
                .contentToString()

    fun addSurfaceLayer(
            tag: String?,
            onSurfaceAvailable: (Surface) -> Unit,
            position: Int,
            onFrameAvailable: (() -> Unit)?
    ): Transformable =
            SurfaceLayer(getNextId(), tag, refineAddPosition(position), this, onSurfaceAvailable, onFrameAvailable)
                .also { addLayer(it) }

    fun addBitmapLayer(tag: String?, bitmap: Bitmap, position: Int): Transformable =
            BitmapLayer(getNextId(), tag, refineAddPosition(position), this, bitmap)
                .also { addLayer(it) }

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

    private fun addLayer(layer: Layer) {
        layer.setup()
        layer.onViewportUpdated(viewport)

        layers.forEach {
            if (it.position >= layer.position) {
                it.position += 1
            }
        }

        layers.add(layer)
        layers.sortBy { it.position }

        addLayerAction?.invoke(layer)

        invalidate()
    }

    private fun moveLayer(layer: Layer, position: Int) {
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

    private fun getNextId() = nextId++

    private fun refineAddPosition(desiredPos: Int): Int {
        return if (desiredPos < 0) {
            0
        } else {
            desiredPos
        }
    }
}