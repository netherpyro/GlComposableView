package com.netherpyro.glcv.layer

import android.graphics.RectF
import android.opengl.Matrix
import android.util.Log
import androidx.annotation.ColorInt
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.Invalidator
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.shader.GlBorderShader
import com.netherpyro.glcv.shader.GlShader
import kotlin.math.abs

/**
 * @author mmikhailov on 2019-11-30.
 */
internal abstract class Layer(
        override val id: Int,
        override val tag: String? = null,
        var position: Int,
        protected val invalidator: Invalidator
) : Transformable {

    companion object {
        val TAG = this::class.java.canonicalName
    }

    override var enableGesturesTransform: Boolean = false

    protected abstract val shader: GlShader
    private val borderShader = GlBorderShader()

    private val frustumRect = RectF()

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
            borderShader.setAspect(value)
        }

    private var shouldDraw = true

    private var glTranslationX = 0f
    private var glTranslationY = 0f

    private var scaleFactor = 1f
    private var rotationDeg = 0f
    private var translationX = 0f // pixels
    private var translationY = 0f // pixels
    private var transFactorX = 0f
    private var transFactorY = 0f

    private lateinit var viewport: GlViewport

    private var initialized = false

    fun setup() {
        onSetup()
        borderShader.setup()
        initialized = true
    }

    fun draw() {
        // Thread.sleep(4L) // TODO investigate why layers in stack (from 6 pieces) flicker if remove this sleep
        // Log.d("Layer","draw::draw ? $shouldDraw::$this")
        if (shouldDraw) {
            onDrawFrame()

            if (borderShader.width > 0f) {
                borderShader.draw(mvpMatrix, aspect)
            }
        }
    }

    fun release() {
        initialized = false
        borderShader.release()
        onRelease()
    }

    protected abstract fun onSetup()
    protected abstract fun onDrawFrame()
    protected abstract fun onRelease()

    override fun setScale(scaleFactor: Float) = doIfInitialized {
        this.scaleFactor = scaleFactor
        borderShader.setScale(scaleFactor)

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setRotation(rotationDeg: Float) = doIfInitialized {
        this.rotationDeg = rotationDeg

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setTranslation(x: Float, y: Float) = doIfInitialized {
        val availWidth = viewport.width.toFloat()
        val availHeight = viewport.height.toFloat()

        translationX = x.coerceIn(-availWidth, availWidth)
        translationY = y.coerceIn(-availHeight, availHeight)
        transFactorX = translationX / viewport.width
        transFactorY = translationY / viewport.height

        glTranslationX = translationX.toGlTranslationX()
        glTranslationY = translationY.toGlTranslationY()

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setTranslationFactor(xFactor: Float, yFactor: Float) = doIfInitialized {
        transFactorX = -xFactor
        transFactorY = yFactor

        translationX = -xFactor * viewport.width
        translationY = yFactor * viewport.height

        glTranslationX = translationX.toGlTranslationX()
        glTranslationY = translationY.toGlTranslationY()

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setOpacity(opacity: Float) = doIfInitialized {
        shader.opacity = opacity

        invalidator.invalidate()
    }

    override fun setBorder(width: Float, @ColorInt color: Int) = doIfInitialized {
        borderShader.width = width
        borderShader.color = color

        invalidator.invalidate()
    }

    override fun setSkipDraw(skip: Boolean) = doIfInitialized {
        shouldDraw = !skip
    }

    override fun setLayerPosition(position: Int) = doIfInitialized {
        invalidator.claimLayerPosition(this, position)
    }

    override fun setSize(width: Float, height: Float) {
        // no-op by default
    }

    override fun getRotation() = rotationDeg
    override fun getScale() = scaleFactor
    override fun getTranslation() = translationX to translationY
    override fun getTranslationFactor() = -(translationX / viewport.width) to (translationY / viewport.height)
    override fun getFrustumRect() = frustumRect
    override fun getLayerAspect() = aspect
    override fun getLayerPosition() = position

    override fun toString(): String {
        return """
            
            /////////////
            class:      ${this.javaClass.name}
            id:         $id,
            tag:        $tag,
            position:   $position
            
        """.trimIndent()
    }

    fun onViewportUpdated(viewport: GlViewport) {
        this.viewport = viewport

        recalculateFrustum()
    }

    protected fun recalculateFrustum() {
        val viewportAspect = viewport.width / viewport.height.toFloat()
        val s: Float = aspect / viewportAspect

        var left = -1.0f
        var top = 1.0f
        var right = 1.0f
        var bottom = -1.0f

        if (viewportAspect >= 1f) { // horizontal viewport
            if (aspect <= 1f) { // vertical content
                left *= viewportAspect
                right *= viewportAspect
            } else { // horizontal content
                if (s > 1.0f) {
                    left *= aspect
                    right *= aspect
                    top *= s
                    bottom *= s
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
                    top *= s
                    bottom *= s
                }
            } else { // horizontal content
                left *= aspect
                right *= aspect

                if (s > 1.0f) {
                    top *= s
                    bottom *= s
                }
            }
        }

        frustumRect.left = left
        frustumRect.top = top
        frustumRect.right = right
        frustumRect.bottom = bottom

        translationX = transFactorX * viewport.width
        translationY = transFactorY * viewport.height

        recalculateMatrices()
    }

    private fun recalculateMatrices() {
        Matrix.frustumM(pMatrix, 0, frustumRect.left, frustumRect.right, frustumRect.bottom, frustumRect.top, 5f, 7f)
        Matrix.setIdentityM(mMatrix, 0)
        Matrix.scaleM(mMatrix, 0, scaleFactor, scaleFactor, 0f)
        Matrix.translateM(mMatrix, 0, glTranslationX, glTranslationY, 0f)
        Matrix.rotateM(mMatrix, 0, rotationDeg, 0f, 0f, 1f)

        Matrix.multiplyMM(mvpMatrix, 0, vMatrix, 0, mMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, mvpMatrix, 0)
    }

    private fun Float.toGlTranslationX(): Float {
        return -(this * (abs(frustumRect.right) + abs(frustumRect.left)) / viewport.width) / scaleFactor
    }

    private fun Float.toGlTranslationY(): Float {
        return (this * (abs(frustumRect.top) + abs(frustumRect.bottom)) / viewport.height) / scaleFactor
    }

    private inline fun doIfInitialized(block: () -> Unit) {
        if (!initialized) {
            Log.w(TAG, "Transformable was not initialized!")
            return
        }

        block()
    }
}