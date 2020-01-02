package com.netherpyro.glcv.touches

import android.content.Context
import android.util.Log
import android.util.SizeF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.netherpyro.glcv.Observable
import com.netherpyro.glcv.Transformable

/**
 * @author mmikhailov on 2019-12-18.
 */
internal class GlTouchHelper(context: Context, transformableObservable: Observable) {

    var viewportSize = SizeF(0f, 0f)

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
            val xRestrictPx = viewportSize.width * leftRightCoeff
            val yRestrictPx = viewportSize.height * topBottomCoeff

            val translationX = (curX + distanceX).coerceIn(-xRestrictPx, xRestrictPx)
            val translationY = (curY + distanceY).coerceIn(-yRestrictPx, yRestrictPx)

            Log.v("Touches", "trX=$translationX, trY=$translationY, vpSize=$viewportSize")

            transformable.setTranslation(translationX, translationY)

            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        val rotation = rotationGestureDetector.onTouchEvent(event)
        val scale = scaleGestureDetector.onTouchEvent(event)
        val translate = gestureDetector.onTouchEvent(event)

        return rotation || scale || translate
    }
}