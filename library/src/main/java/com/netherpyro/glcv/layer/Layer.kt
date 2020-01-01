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

            if (!aspectSet) {
                aspectSet = true
                aspectReadyAction?.invoke(value)
            }
        }

    private var shouldDraw = true

    private var glTranslationX = 0f
    private var glTranslationY = 0f

    private var scaleFactor = 1f
    private var rotationDeg = 0f
    private var translationX = 0f // pixels
    private var translationY = 0f // pixels

    private var aspectReadyAction: ((Float) -> Unit)? = null
    private var aspectSet = false

    private lateinit var viewport: GlViewport

    fun setup() {
        onSetup()
        borderShader.setup()
    }

    fun draw() {
        if (shouldDraw) {
            onDrawFrame()

            if (borderShader.width > 0f) {
                borderShader.draw(mvpMatrix, aspect)
            }
        }
    }

    fun release() {
        borderShader.release()
        onRelease()
    }

    protected abstract fun onSetup()
    protected abstract fun onDrawFrame()
    protected abstract fun onRelease()

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
        this.glTranslationX = x.toGlTranslationX()
        this.glTranslationY = y.toGlTranslationY()

        recalculateMatrices()
        invalidator.invalidate()
    }

    override fun setOpacity(opacity: Float) {
        shader.opacity = opacity

        invalidator.invalidate()
    }

    override fun setBorder(width: Float, @ColorInt color: Int) {
        borderShader.width = width
        borderShader.color = color

        invalidator.invalidate()
    }

    override fun setSkipDraw(skip: Boolean) {
        shouldDraw = !skip
    }

    override fun setLayerPosition(position: Int) {
        invalidator.claimPosition(this, position)
    }

    override fun getRotation() = rotationDeg
    override fun getScale() = scaleFactor
    override fun getTranslation() = translationX to translationY
    override fun getFrustumRect() = frustumRect
    override fun getLayerAspect() = aspect

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

        recalculateMatrices()
    }

    fun listenAspectRatioReady(onReadyAction: (Float) -> Unit) {
        if (aspectSet) onReadyAction(aspect)
        else aspectReadyAction = onReadyAction
    }

    // todo divide into 2 functions
    protected fun recalculateMatrices() {
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

        Log.d("Layer", "l=$left, t=$top, r=$right, b=$bottom")
        Log.i("Layer", "glTrX=$glTranslationX, glTrY=$glTranslationY")

        Matrix.frustumM(pMatrix, 0, left, right, bottom, top, 5f, 7f)
        Matrix.setIdentityM(mMatrix, 0)
        Matrix.scaleM(mMatrix, 0, scaleFactor, scaleFactor, 0f)
        Matrix.rotateM(mMatrix, 0, rotationDeg, 0f, 0f, 1f)
        Matrix.translateM(mMatrix, 0, glTranslationX, glTranslationY, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, vMatrix, 0, mMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, pMatrix, 0, mvpMatrix, 0)
    }

    // todo consider scale factor
    private fun Float.toGlTranslationX(): Float {
        return -(this * (abs(frustumRect.right) + abs(frustumRect.left)) / viewport.width)
    }

    private fun Float.toGlTranslationY(): Float {
        return (this * (abs(frustumRect.top) + abs(frustumRect.bottom)) / viewport.height)
    }
}