package com.netherpyro.glcv.layer

import android.opengl.Matrix
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.shader.GlShader
import timber.log.Timber

/**
 * @author mmikhailov on 2019-11-30.
 */
internal abstract class Layer(protected val invalidator: Invalidator) {

    protected val mvpMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val mMatrix = FloatArray(16)
    private val vMatrix = FloatArray(16)

    init {
        Matrix.setLookAtM(vMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        )
    }

    protected var aspect: Float = 1f
    protected abstract var shader: GlShader

    private var scaleFactor: Float = 1f
    private var rotationDeg = 0
    private var translationX = 0
    private var translationY = 0
    private var viewportAspect = 1f

    abstract fun onGlPrepared()
    abstract fun onDrawFrame()

    open fun release() {
        shader.release()
    }

    fun setScale(scaleFactor: Float) {
        this.scaleFactor = scaleFactor

        recalculateMatrices()
        invalidator.invalidate()
    }

    fun setRotation(rotation: Int) {
        this.rotationDeg = rotation

        recalculateMatrices()
        invalidator.invalidate()
    }

    fun setTranslation(x: Int, y: Int) {
        this.translationX = x
        this.translationY = y

        recalculateMatrices()
        invalidator.invalidate()
    }

    fun onViewportAspectRatioChanged(aspect: Float) {
        viewportAspect = aspect

        recalculateMatrices()
    }

    protected fun recalculateMatrices() {
        val s: Float = aspect / viewportAspect

        var left = -1.0f
        var top = 1.0f
        var right = 1.0f
        var bottom = -1.0f
        val viewportHorizontal: Boolean = viewportAspect >= 1f
        val videoHorizontal = aspect > 1f

        Timber.d("changeVideoSize::viewportHorizontal? $viewportHorizontal, videoHorizontal? $videoHorizontal")

        if (viewportHorizontal) { // horizontal viewport
            if (aspect <= 1f) { // vertical content
                left *= viewportAspect
                right *= viewportAspect
            } else { // horizontal content
                if (s > 1.0f) {
                    left *= aspect
                    right *= aspect
                    top *= aspect * 1f / viewportAspect
                    bottom *= aspect * 1f / viewportAspect
                } else {
                    left *= viewportAspect
                    right *= viewportAspect
                }
            }
        } else { // vertical viewport
            if (aspect < 1f) { // vertical content
                if (s < 1.0f) {
                    left *= viewportAspect
                    right *= viewportAspect
                } else {
                    left *= aspect
                    right *= aspect
                    top *= aspect * 1f / viewportAspect
                    bottom *= aspect * 1f / viewportAspect
                }
            } else { // horizontal content
                left *= aspect
                right *= aspect
                if (s > 1.0f) {
                    top *= aspect * 1f / viewportAspect
                    bottom *= aspect * 1f / viewportAspect
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