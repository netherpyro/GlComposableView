package com.netherpyro.glcv.layer

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.view.Surface
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.VideoTransformable
import com.netherpyro.glcv.shader.GlExtTextureShader
import com.netherpyro.glcv.util.EglUtil

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class VideoLayer(
        id: Int,
        tag: String?,
        position: Int,
        invalidator: Invalidator,
        private val onSurfaceAvailable: (Surface) -> Unit
) : Layer(id, tag, position, invalidator), VideoTransformable, OnFrameAvailableListener {

    override val shader = GlExtTextureShader()

    private lateinit var surfaceTexture: SurfaceTexture

    private var updateTexImageCounter = 0
    private var texName = EglUtil.NO_TEXTURE

    private val transformMatrix = FloatArray(16)
    private val textureTarget = GlExtTextureShader.GL_TEXTURE_EXTERNAL_OES

    private var initialized = false
    private var pendingCalculate = false

    /**
     * Must be called in GL thread
     * */
    override fun onSetup() {
        release()

        texName = EglUtil.genBlankTexture(textureTarget)

        surfaceTexture = SurfaceTexture(texName)
        surfaceTexture.setOnFrameAvailableListener(this)

        shader.setup()

        onSurfaceAvailable(Surface(surfaceTexture))

        synchronized(this) {
            updateTexImageCounter = 0
            initialized = true

            if (pendingCalculate) {
                pendingCalculate = false
                recalculateFrustum()
                invalidator.invalidate()
            }
        }
    }

    @Synchronized
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        updateTexImageCounter++

        invalidator.invalidate()
    }

    /**
     * Must be called in GL thread
     * */
    @Synchronized
    override fun onDrawFrame() {
        while (updateTexImageCounter != 0) {
            surfaceTexture.updateTexImage()
            surfaceTexture.getTransformMatrix(transformMatrix)
            updateTexImageCounter--
        }

        shader.draw(texName, mvpMatrix, transformMatrix, aspect)
    }

    /**
     * Must be called in GL thread
     * */
    override fun onRelease() {
        initialized = false
        shader.release()

        EglUtil.deleteTextures(texName)
    }

    override fun setVideoSize(width: Float, height: Float) {
        aspect = width / height

        if (initialized) {
            recalculateFrustum()
            invalidator.invalidate()
        } else {
            pendingCalculate = true
        }
    }
}