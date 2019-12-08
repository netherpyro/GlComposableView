package com.netherpyro.glcv.layer

import android.opengl.Matrix
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.shader.GlShader

/**
 * @author mmikhailov on 2019-11-30.
 */
internal abstract class Layer(override val id: Int, protected val invalidator: Invalidator) : Transformable {

    protected abstract val shader: GlShader

    protected val mvpMatrix = FloatArray(16)

    private val pMatrix = FloatArray(16)
    private val mMatrix = FloatArray(16)
    private val vMatrix = FloatArray(16).apply {
        Matrix.setLookAtM(this, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        )
    }

    protected var aspect: Float = 1f
        set(value) {
            field = value

            if (!aspectSet) {
                aspectSet = true
                aspectReadyAction?.invoke(value)
            }
        }

    private var scaleFactor = 1f
    private var rotationDeg = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var viewportAspect = 1f

    private var aspectReadyAction: ((Float) -> Unit)? = null
    private var aspectSet = false

    abstract fun onGlPrepared()
    abstract fun onDrawFrame()
    abstract fun release()

    override fun setScale(scaleFactor: Float) {
        this.scaleFactor = scaleFactor

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setRotation(rotationDeg: Float) {
        this.rotationDeg = rotationDeg

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setTranslation(x: Float, y: Float) {
        this.translationX = x
        this.translationY = y

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setOpacity(opacity: Float) {
        shader.opacity = opacity

        invalidator.invalidate()
    }

    fun onViewportAspectRatioChanged(aspect: Float) {
        viewportAspect = aspect

        recalculateMatrices()
    }

    fun listenAspectRatioReady(onReadyAction: (Float) -> Unit) {
        if (aspectSet) onReadyAction(aspect)
        else aspectReadyAction = onReadyAction
    }

    protected fun recalculateMatrices() {
        val s: Float = aspect / viewportAspect

        var left = -1.0f
        var top = 1.0f
        var right = 1.0f
        var bottom = -1.0f
        val viewportHorizontal: Boolean = viewportAspect >= 1f

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
        Matrix.scaleM(mMatrix, 0, scaleFactor, scaleFactor, 0f)
        Matrix.rotateM(mMatrix, 0, rotationDeg, 0f, 0f, 1f)
        Matrix.translateM(mMatrix, 0, translationX, translationY, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, vMatrix, 0, mMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, mvpMatrix, 0)
    }
}