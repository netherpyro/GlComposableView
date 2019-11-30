package com.netherpyro.glcv.layer

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGenTextures
import android.opengl.Matrix
import android.view.Surface
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.drawer.GlExtTextureDrawer
import com.netherpyro.glcv.util.EglUtil
import timber.log.Timber

/**
 * @author mmikhailov on 2019-11-30.
 */
internal class VideoLayer(
        private val player: SimpleExoPlayer,
        invalidator: Invalidator
) : Layer(invalidator), VideoListener, OnFrameAvailableListener {

    init {
        player.addVideoListener(this)
    }

    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var videoDrawer: GlExtTextureDrawer

    private var updateSurface = false

    private var texName = EglUtil.NO_TEXTURE

    private val mvpMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val mMatrix = FloatArray(16)
    private val vMatrix = FloatArray(16)
    private val transformMatrix = FloatArray(16)
    private val textureTarget = GlExtTextureDrawer.GL_TEXTURE_EXTERNAL_OES

    private var scaleFactor = 1f
    private var rotation = 0
    private var translationX = 0
    private var translationY = 0
    private var videoAspect = 1f
    private var viewportAspect = 1f

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

        videoDrawer = GlExtTextureDrawer()
        videoDrawer.setup()

        val surface = Surface(surfaceTexture)
        player.setVideoSurface(surface)

        Matrix.setLookAtM(vMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        )

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

        videoDrawer.draw(texName, mvpMatrix, transformMatrix, videoAspect)
    }

    override fun onViewportAspectRatioChanged(aspect: Float) {
        viewportAspect = aspect

        recalculateMatrices()
    }

    override fun setScale(scaleFactor: Float) {
        this.scaleFactor = scaleFactor

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setRotation(rotation: Int) {
        this.rotation = rotation

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setTranslation(x: Int, y: Int) {
        this.translationX = x
        this.translationY = y

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun release() {
        surfaceTexture.release()
    }

    private fun onVideoSizeChanged(videoW: Float, videoH: Float) {
        Timber.d("changeVideoSize::video w: $videoW, h: $videoH")

        videoAspect = videoW / videoH

        recalculateMatrices()
        invalidator.invalidate()
    }

    private fun recalculateMatrices() {
        val s: Float = videoAspect / viewportAspect

        var left = -1.0f
        var top = 1.0f
        var right = 1.0f
        var bottom = -1.0f
        val viewportHorizontal: Boolean = viewportAspect >= 1f
        val videoHorizontal = videoAspect > 1f

        Timber.d("changeVideoSize::viewportHorizontal? $viewportHorizontal, videoHorizontal? $videoHorizontal")

        if (viewportHorizontal) { // horizontal viewport
            if (videoAspect <= 1f) { // vertical video
                left *= viewportAspect // ok
                right *= viewportAspect // ok
            } else { // horizontal video
                if (s > 1.0f) {
                    left *= videoAspect // ok
                    right *= videoAspect // ok
                    top *= videoAspect * 1f / viewportAspect // ok
                    bottom *= videoAspect * 1f / viewportAspect // ok
                } else {
                    left *= viewportAspect // ok
                    right *= viewportAspect // ok
                }
            }
        } else { // vertical viewport
            if (videoAspect < 1f) { // vertical video
                if (s < 1.0f) {
                    left *= viewportAspect // ok
                    right *= viewportAspect // ok
                } else {
                    left *= videoAspect // ok
                    right *= videoAspect // ok
                    top *= videoAspect * 1f / viewportAspect // ok
                    bottom *= videoAspect * 1f / viewportAspect // ok
                }
            } else { // horizontal video
                left *= videoAspect
                right *= videoAspect
                if (s > 1.0f) {
                    top *= videoAspect * 1f / viewportAspect // ok
                    bottom *= videoAspect * 1f / viewportAspect // ok
                }
            }
        }
        Matrix.frustumM(pMatrix, 0, left, right, bottom, top, 5f, 7f)
        Matrix.setIdentityM(mMatrix, 0)
//        Matrix.scaleM(MMatrix, 0, 1.1f, 1.1f, 0); // todo scale
//        Matrix.rotateM(MMatrix, 0, 38f, 0, 0, 1); // todo rotation
//        Matrix.translateM(MMatrix, 0, 0.1f, 0.1f, 0); // todo translation

        Matrix.multiplyMM(mvpMatrix, 0, vMatrix, 0, mMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, mvpMatrix, 0)
    }
}