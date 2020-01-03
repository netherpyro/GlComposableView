package com.netherpyro.glcv.touches

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.Observable
import com.netherpyro.glcv.Transformable
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author mmikhailov on 2019-12-18.
 */
internal class GlTouchHelper(context: Context, transformableObservable: Observable) {

    var viewport = GlViewport()

    private var currentTransformableId = 0

    private val transformables = mutableMapOf<Int, Transformable>()
    private val maxScale = 2f
    private val minScale = 0.5f

    init {
        val existingTransformables: List<Transformable>

        transformableObservable.subscribeLayersChange(
                addAction = { transformable -> transformables[transformable.id] = transformable },
                removeAction = { transformable -> transformables.remove(transformable) }
        )
            .also { transformables -> existingTransformables = transformables }

        transformables.putAll(
                existingTransformables.map { transformable -> transformable.id to transformable }
        )
    }

    private val rotationGestureDetector = RotationGestureDetector { angle ->
        transformables[currentTransformableId]!!.setRotation(angle)
    }

    private val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val transformable = transformables[currentTransformableId]!!
                    val scaleFactor = (transformable.getScale() * detector.scaleFactor)
                        .coerceIn(minScale, maxScale)

                    transformable.setScale(scaleFactor)

                    return true
                }
            })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            val transformable = transformables[currentTransformableId]!!
            val (curX, curY) = transformable.getTranslation()
            val scaleFactor = transformable.getScale()

            val layerFrustum = transformable.getFrustumRect()
            val layerAspect = transformable.getLayerAspect()
            val leftRightCoeff = 0.5f + 1f / (layerFrustum.right - layerFrustum.left) * layerAspect * scaleFactor
            val topBottomCoeff = 0.5f + 1f / (layerFrustum.top - layerFrustum.bottom) * scaleFactor

            // todo add option to set restriction factor
            val xRestrictPx = viewport.width * leftRightCoeff
            val yRestrictPx = viewport.height * topBottomCoeff

            val translationX = (curX + distanceX).coerceIn(-xRestrictPx, xRestrictPx)
            val translationY = (curY + distanceY).coerceIn(-yRestrictPx, yRestrictPx)

            transformable.setTranslation(translationX, translationY)

            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            var tapConsumed = false

            val tapX = e.x
            val tapY = e.y
            val vpWidthRange = viewport.x.toFloat()..(viewport.x.toFloat() + viewport.width)
            val vpHeightRange = viewport.y.toFloat()..(viewport.y.toFloat() + viewport.height)

            if (tapX in vpWidthRange && tapY in vpHeightRange) {

                Log.v("Touches", "tap inside viewport!")

                for (transformableEntry in transformables) {
                    if ((tapX to tapY).hitTest(transformableEntry.value)) {
                        Log.i("Touches", "Hit! the transformable ${transformableEntry.value}")
                        currentTransformableId = transformableEntry.key
                        tapConsumed = true
                        break
                    }
                }
            }

            return tapConsumed
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        val rotation = rotationGestureDetector.onTouchEvent(event)
        val scale = scaleGestureDetector.onTouchEvent(event)
        val translate = gestureDetector.onTouchEvent(event)

        return rotation || scale || translate
    }

    private fun Pair<Float, Float>.hitTest(transformable: Transformable): Boolean {
        val tapPoint = PointF(first, second)

        val (trX, trY) = transformable.getTranslation()
        val scaleFactor = transformable.getScale()
        val layerFrustum = transformable.getFrustumRect()
        val layerAspect = transformable.getLayerAspect()
        val layerAngleRad = Math.toRadians(transformable.getRotation().toDouble())

        val layerWidth = 1f / (layerFrustum.right - layerFrustum.left) * layerAspect * scaleFactor * 2f * viewport.width
        val layerHeight = 1f / (layerFrustum.top - layerFrustum.bottom) * scaleFactor * 2f * viewport.height

        val layerLeftTop = PointF(
                viewport.x + viewport.width / 2f - layerWidth / 2f - trX,
                viewport.y + viewport.height / 2f - layerHeight / 2f - trY
        )

        val layerRect = RectF(
                layerLeftTop.x,
                layerLeftTop.y,
                layerLeftTop.x + layerWidth,
                layerLeftTop.y + layerHeight
        )

        if (layerAngleRad == 0.0) {
            return layerRect.contains(tapPoint.x, tapPoint.y)
        } else {

            val rotationCenterPoint = PointF(layerRect.centerX(), layerRect.centerY())
            tapPoint.offset(-rotationCenterPoint.x, -rotationCenterPoint.y)

            val sinus = sin(layerAngleRad).toFloat()
            val cosinus = cos(layerAngleRad).toFloat()

            val rotatedTapPoint = PointF(
                    tapPoint.x * cosinus - tapPoint.y * sinus,
                    tapPoint.x * sinus + tapPoint.y * cosinus
            )
                .apply { offset(rotationCenterPoint.x, rotationCenterPoint.y) }

            Log.w("Touches", """
                
                hitTest::
                tapPoint=$tapPoint
                layerWidth=$layerWidth
                layerHeight= $layerHeight
                layerRotDeg=${transformable.getRotation()}
                origLeftTop=$layerLeftTop
                rotatedTap=$rotatedTapPoint
                trX=$trX
                trY=$trY
                
            """.trimIndent())

            return layerRect.contains(rotatedTapPoint.x, rotatedTapPoint.y)
        }
    }
}