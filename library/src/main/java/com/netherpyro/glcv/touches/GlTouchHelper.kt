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

    var maxScale = 2f
    var minScale = 0.5f

    var useIteration: Boolean = false

    private var prevHits = mutableMapOf<Transformable, Boolean>()
    private val transformables = mutableListOf<Transformable>()

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
                    transformable.setTranslation(curX + distanceX, curY + distanceY)
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

                if (!useIteration) {
                    for (transformable in transformables) {
                        if (hitTest(PointF(e.x, (viewHeight - e.y)), transformable)) {
                            Log.i("Touches", "Hit! the transformable $transformable")
                            hitLayer = transformable
                            break
                        }
                    }
                } else {
                    val allHits = mutableListOf<Transformable>()
                    for (transformable in transformables) {
                        if (hitTest(PointF(e.x, (viewHeight - e.y)), transformable)) {
                            allHits.add(transformable)
                        }
                    }

                    when {
                        prevHits.keys.containsAll(allHits) && prevHits.keys.size == allHits.size -> { // iterate
                            if (prevHits.values.all { it }) {
                                prevHits = prevHits.keys.associateWith { false }.toMutableMap()
                            }

                            for ((transformable, hit) in prevHits.entries) {
                                if (!hit) {
                                    hitLayer = transformable
                                    prevHits[transformable] = true
                                    Log.i("Touches", "Hit! the transformable $hitLayer")
                                    break
                                }
                            }
                        }
                        allHits.isNotEmpty() -> { // pick the first hit test result
                            prevHits = allHits.associateWith { false }.toMutableMap()
                            hitLayer = allHits.first()
                            prevHits[hitLayer] = true
                            Log.i("Touches", "Hit! the transformable $hitLayer")
                        }
                        else -> { // hit test failed
                            prevHits.clear()
                            hitLayer = null
                        }
                    }
                }

                return if (hitLayer != null) {
                    touchesListener?.onLayerTap(hitLayer) ?: false
                } else {
                    touchesListener?.onViewportInsideTap() ?: false
                }
            }

            prevHits.clear()
            return touchesListener?.onViewportOutsideTap() ?: false
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        val prevRotationAngle = transformables.firstOrNull { it.enableGesturesTransform }?.getRotation()
        val rotation = rotationGestureDetector.onTouchEvent(event, prevRotationAngle)
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