package com.netherpyro.glcv.layer

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGenTextures
import android.view.Surface
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.shader.GlExtTextureShader
import com.netherpyro.glcv.shader.GlShader
import com.netherpyro.glcv.util.EglUtil
import timber.log.Timber

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class ExoPLayer(
        private val player: SimpleExoPlayer,
        invalidator: Invalidator
) : Layer(invalidator), VideoListener, OnFrameAvailableListener {

    init {
        player.addVideoListener(this)
    }

    override lateinit var shader: GlShader
    private lateinit var surfaceTexture: SurfaceTexture

    private var updateSurface = false
    private var texName = EglUtil.NO_TEXTURE
    
    private val transformMatrix = FloatArray(16)
    private val textureTarget = GlExtTextureShader.GL_TEXTURE_EXTERNAL_OES

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                    pixelWidthHeightRatio: Float) {
        onVideoSizeChanged(width * pixelWidthHeightRatio, height * pixelWidthHeightRatio)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        updateSurface = true
        invalidator.invalidate()
    }

    /**
     * Should be called in GL thread
     * */
    override fun onGlPrepared() {
        val args = IntArray(1)

        glGenTextures(args.size, args, 0)
        texName = args[0]

        surfaceTexture = SurfaceTexture(texName)
        surfaceTexture.setOnFrameAvailableListener(this)

        glBindTexture(textureTarget, texName)
        EglUtil.setupSampler(textureTarget, GLES20.GL_LINEAR, GLES20.GL_NEAREST)
        glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        shader = GlExtTextureShader()
        shader.setup()

        val surface = Surface(surfaceTexture)
        player.setVideoSurface(surface)

        synchronized(this) { updateSurface = false }
    }

    /**
     * Should be called in GL thread
     * */
    override fun onDrawFrame() {
        synchronized(this) {
            if (updateSurface) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(transformMatrix)
                updateSurface = false
            }
        }

        (shader as GlExtTextureShader).draw(texName, mvpMatrix, transformMatrix, aspect)
    }

    override fun release() {
        super.release()

        EglUtil.deleteTextures(texName)
    }

    private fun onVideoSizeChanged(videoW: Float, videoH: Float) {
        Timber.d("changeVideoSize::video w: $videoW, h: $videoH")

        aspect = videoW / videoH

        recalculateMatrices()
        invalidator.invalidate()
    }


}