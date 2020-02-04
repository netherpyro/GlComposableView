package com.netherpyro.glcv.touches

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.TransformableObservable
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author mmikhailov on 2019-12-18.
 */
internal class GlTouchHelper(context: Context, observable: TransformableObservable) {

    var viewport = GlViewport()
    var viewHeight: Int = 0

    var touchesListener: LayerTouchListener? = null

    private val transformables = mutableListOf<Transformable>()
    private val maxScale = 2f
    private val minScale = 0.5f

    init {
        val existingTransformables: List<Transformable>

        observable.subscribeLayersChange(
                addAction = { transformable ->
                    transformables.add(transformable)
                    transformables.sortByDescending { it.getLayerPosition() }
                },
                removeAction = { transformableId ->
                    transformables.removeAll { it.id == transformableId }
                    transformables.sortByDescending { it.getLayerPosition() }
                },
                changeLayerPositionsAction = {
                    transformables.sortByDescending { it.getLayerPosition() }
                }
        )
            .also { transformables -> existingTransformables = transformables }

        transformables.addAll(existingTransformables)
        transformables.sortByDescending { it.getLayerPosition() }
    }

    // rotation
    private val rotationGestureDetector = RotationGestureDetector { angle ->
        transformables.filter { it.enableGesturesTransform }
            .forEach { it.setRotation(angle) }
    }

    // scale
    private val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {

                    transformables.filter { it.enableGesturesTransform }
                        .forEach { transformable ->

                            val scaleFactor = (transformable.getScale() * detector.scaleFactor)
                                .coerceIn(minScale, maxScale)

                            transformable.setScale(scaleFactor)
                        }

                    return true
                }
            })

    // pan & click
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            transformables.filter { it.enableGesturesTransform }
                .forEach { transformable ->

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
                }

            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (touchesListener == null) return false

            val vpWidthRange = viewport.x.toFloat()..(viewport.x.toFloat() + viewport.width)
            val vpHeightRange = viewport.y.toFloat()..(viewport.y.toFloat() + viewport.height)

            if (e.x in vpWidthRange && (viewHeight - e.y) in vpHeightRange) {

                Log.v("Touches", "tap inside viewport!")

                var hitLayer: Transformable? = null

                for (transformable in transformables) {
                    if (hitTest(PointF(e.x, (viewHeight - e.y)), transformable)) {
                        Log.i("Touches", "Hit! the transformable $transformable")
                        hitLayer = transformable
                        break
                    }
                }

                return if (hitLayer != null) {
                    touchesListener?.onLayerTap(hitLayer) ?: false
                } else {
                    touchesListener?.onViewportInsideTap() ?: false
                }
            }

            return touchesListener?.onViewportOutsideTap() ?: false
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        val rotation = rotationGestureDetector.onTouchEvent(event)
        val scale = scaleGestureDetector.onTouchEvent(event)
        val translate = gestureDetector.onTouchEvent(event)

        return rotation || scale || translate
    }

    private fun hitTest(tapPoint: PointF, transformable: Transformable): Boolean {
        val (trX, trY) = transformable.getTranslation()
        val scaleFactor = transformable.getScale()
        val layerFrustum = transformable.getFrustumRect()
        val layerAspect = transformable.getLayerAspect()
        val layerAngleRad = Math.toRadians(transformable.getRotation().toDouble())

        val layerWidth = 1f / (layerFrustum.right - layerFrustum.left) * layerAspect * scaleFactor * 2f * viewport.width
        val layerHeight = 1f / (layerFrustum.top - layerFrustum.bottom) * scaleFactor * 2f * viewport.height

        val layerLeftTop = PointF(
                viewport.x + viewport.width / 2f - layerWidth / 2f - trX,
                viewport.y + viewport.height / 2f - layerHeight / 2f + trY
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