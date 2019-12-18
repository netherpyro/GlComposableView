package com.netherpyro.glcv.touches

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.netherpyro.glcv.Observable

/**
 * @author mmikhailov on 2019-12-18.
 */
internal class GlTouchHelper(context: Context, transformableObservable: Observable) {

    private val touchables = mutableMapOf<Int, Touchable>()
    private val maxScale = 2f
    private val minScale = 0.5f

    init {
        val existingTransformables = transformableObservable.subscribe(
                addAction = { touchables[it.id] = Touchable(transformable = it) },
                removeAction = { touchables.remove(it) }
        )

        touchables.putAll(existingTransformables.map { it.id to Touchable(transformable = it) })
    }

    private val rotationGestureDetector = RotationGestureDetector {
        touchables[currentTransformableId]?.rotationAngle = it
        touchables[currentTransformableId]?.transformable?.setRotation(it)
    }

    private val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = touchables[currentTransformableId]?.scaleFactor
                        ?.let { (it * detector.scaleFactor).coerceIn(minScale, maxScale) }
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