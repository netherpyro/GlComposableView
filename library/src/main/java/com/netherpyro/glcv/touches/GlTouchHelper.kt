package com.netherpyro.glcv.touches

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.netherpyro.glcv.Observable
import com.netherpyro.glcv.Transformable

/**
 * @author mmikhailov on 2019-12-18.
 */
internal class GlTouchHelper(context: Context, transformableObservable: Observable) {

    private val touchables = mutableMapOf<Int, Touchable>()
    private val maxScale = 2f
    private val minScale = 0.5f

    init {
        val existingTransformables: List<Transformable>

        transformableObservable.subscribeLayersChange(
                addAction = { transformable ->
                    touchables[transformable.id] = Touchable(transformable = transformable)
                },
                removeAction = { transformable -> touchables.remove(transformable) }
        )
            .also { transformables -> existingTransformables = transformables }

        touchables.putAll(existingTransformables.map { transformable ->
            transformable.id to Touchable(transformable = transformable)
        })
    }

    private val rotationGestureDetector = RotationGestureDetector { angle ->
        touchables[currentTransformableId]?.rotationAngle = angle
        touchables[currentTransformableId]?.transformable?.setRotation(angle)
    }

    private val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = touchables[currentTransformableId]?.scaleFactor
                        ?.let { scaleFactor -> (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale) }
                        ?: 1f

                    touchables[currentTransformableId]?.scaleFactor = scaleFactor
                    touchables[currentTransformableId]?.transformable?.setScale(scaleFactor)

                    return true
                }
            })

    private var currentTransformableId = 0

    fun onTouchEvent(event: MotionEvent): Boolean {
        val rotation = rotationGestureDetector.onTouchEvent(event)
        val scale = scaleGestureDetector.onTouchEvent(event)

        return rotation || scale
    }
}