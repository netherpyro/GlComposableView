package com.netherpyro.glcv.layer

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.view.Surface
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.SurfaceConsumer
import com.netherpyro.glcv.shader.GlExtTextureShader
import com.netherpyro.glcv.util.EglUtil

/**
 * @author mmikhailov on 2019-11-30.
 *
 * Surface layer is used for player frames rendering;
 * Due to we won't learn aspect of video during layer setup there is
 * {@link setSize()} method overridden.
 */
internal class SurfaceLayer(
        id: Int,
        tag: String?,
        position: Int,
        invalidator: Invalidator,
        private val surfaceConsumer: SurfaceConsumer,
        private val onFrameAvailable: ((Long) -> Unit)? = null
) : Layer(id, tag, position, invalidator), OnFrameAvailableListener {

    override val shader = GlExtTextureShader()

    private lateinit var surfaceTexture: SurfaceTexture

    private var updateTexImageCounter = 0
    private var texName = EglUtil.NO_TEXTURE

    private val transformMatrix = FloatArray(16)
    private val textureTarget = GlExtTextureShader.GL_TEXTURE_EXTERNAL_OES

    /**
     * Must be called in GL thread
     * */
    override fun onSetup() {
        release()

        updateTexImageCounter = 0

        texName = EglUtil.genBlankTexture(textureTarget)

        surfaceTexture = SurfaceTexture(texName)
        surfaceTexture.setOnFrameAvailableListener(this)

        shader.setup()

        surfaceConsumer.consume(Surface(surfaceTexture))
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        updateTexImageCounter++

        invalidator.invalidate()
        onFrameAvailable?.invoke(surfaceTexture.timestamp)
    }

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
        shader.release()

        EglUtil.deleteTextures(texName)
    }

    override fun setSize(width: Float, height: Float) {
        aspect = width / height
        recalculateFrustum()
        invalidator.invalidate()
    }
}