package com.netherpyro.glcv.layer

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.view.Surface
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.shader.GlExtTextureShader
import com.netherpyro.glcv.util.EglUtil

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class ExoPLayer(
        id: Int,
        invalidator: Invalidator,
        private val player: SimpleExoPlayer
) : Layer(id, invalidator), VideoListener, OnFrameAvailableListener {

    override val shader = GlExtTextureShader()

    private lateinit var surfaceTexture: SurfaceTexture

    private var updateTexImageCounter = 0
    private var texName = EglUtil.NO_TEXTURE

    private val transformMatrix = FloatArray(16)
    private val textureTarget = GlExtTextureShader.GL_TEXTURE_EXTERNAL_OES

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                    pixelWidthHeightRatio: Float) {
        onVideoSizeChanged(width * pixelWidthHeightRatio, height * pixelWidthHeightRatio)
    }

    override fun onRenderedFirstFrame() {
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            updateTexImageCounter++

            invalidator.invalidate()
        }
    }

    /**
     * Must be called in GL thread
     * */
    override fun setup() {
        release()

        texName = EglUtil.genBlankTexture(textureTarget)

        surfaceTexture = SurfaceTexture(texName)
        surfaceTexture.setOnFrameAvailableListener(this)

        shader.setup()

        player.addVideoListener(this)

        val surface = Surface(surfaceTexture)
        player.setVideoSurface(surface)

        synchronized(this) { updateTexImageCounter = 0 }
    }

    /**
     * Must be called in GL thread
     * */
    override fun onDrawFrame() {
        synchronized(this) {
            while (updateTexImageCounter != 0) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(transformMatrix)
                updateTexImageCounter--
            }

            shader.draw(texName, mvpMatrix, transformMatrix, aspect)
        }
    }

    /**
     * Must be called in GL thread
     * */
    override fun release() {
        player.removeVideoListener(this)
        shader.release()

        EglUtil.deleteTextures(texName)
    }

    private fun onVideoSizeChanged(videoW: Float, videoH: Float) {
        aspect = videoW / videoH

        recalculateMatrices()
        invalidator.invalidate()
    }
}