package com.netherpyro.glcv.touches

import android.view.MotionEvent
import com.netherpyro.glcv.util.Haptic
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Credits https://stackoverflow.com/questions/10682019/android-two-finger-rotation
 * */
internal class RotationGestureDetector(
        private val haptic: Haptic,
        private val listener: (Float) -> Unit
) {

    companion object {
        private const val INVALID_POINTER_ID = -1
    }

    private var fX = 0f
    private var fY = 0f
    private var sX = 0f
    private var sY = 0f
    private var ptrID1: Int
    private var ptrID2: Int

    var angle = 0f
        private set

    var isSnapEnabled: Boolean = false

    private var shouldVibrate = false

    private var oldAngle = 0f

    init {
        ptrID1 = INVALID_POINTER_ID
        ptrID2 = INVALID_POINTER_ID
    }

    fun onTouchEvent(event: MotionEvent, prevAngle: Float? = null): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> ptrID1 = event.getPointerId(event.actionIndex)
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldAngle = prevAngle ?: angle
                ptrID2 = event.getPointerId(event.actionIndex)
                sX = event.getX(event.findPointerIndex(ptrID1))
                sY = event.getY(event.findPointerIndex(ptrID1))
                fX = event.getX(event.findPointerIndex(ptrID2))
                fY = event.getY(event.findPointerIndex(ptrID2))
            }
            MotionEvent.ACTION_MOVE -> if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                val nsX: Float = event.getX(event.findPointerIndex(ptrID1))
                val nsY: Float = event.getY(event.findPointerIndex(ptrID1))
                val nfX: Float = event.getX(event.findPointerIndex(ptrID2))
                val nfY: Float = event.getY(event.findPointerIndex(ptrID2))

                angle = if (isSnapEnabled) {
                    snappingAngle((angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY) + oldAngle) % 360f)
                        .also { angel -> checkVibration(angel) }
                } else {
                    (angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY) + oldAngle) % 360f
                }

                listener(angle)
            }

            MotionEvent.ACTION_UP -> ptrID1 = INVALID_POINTER_ID
            MotionEvent.ACTION_POINTER_UP -> ptrID2 = INVALID_POINTER_ID
            MotionEvent.ACTION_CANCEL -> {
                ptrID1 = INVALID_POINTER_ID
                ptrID2 = INVALID_POINTER_ID
            }
        }

        return true
    }

    private fun checkVibration(angel: Float) {
        if (shouldVibrate && abs(angel % 45) == 0f) {
            shouldVibrate = false
            haptic.perform()
        } else if (abs(angel % 45) != 0f) {
            shouldVibrate = true
        }
    }

    private fun angleBetweenLines(fX: Float, fY: Float, sX: Float, sY: Float,
                                  nfX: Float, nfY: Float, nsX: Float,
                                  nsY: Float): Float {

        val angle1 = atan2((fY - sY).toDouble(), (fX - sX).toDouble())
            .toFloat()
        val angle2 = atan2((nfY - nsY).toDouble(), (nfX - nsX).toDouble())
            .toFloat()
        var angle = Math.toDegrees(angle1 - angle2.toDouble()).toFloat() % 360

        if (angle < -180f) angle += 360.0f
        if (angle > 180f) angle -= 360.0f

        return angle
    }

    private fun snappingAngle(angle: Float): Float {
        val divergence = angle % 45
        return when {
            (0f..4f).contains(divergence) && angle > 0 -> angle - divergence
            (41f..45f).contains(divergence) && angle > 0 -> angle + (45f - divergence)
            (-4f..0f).contains(divergence) && angle < 0 -> angle - divergence
            (-45f..-41f).contains(divergence) && angle < 0 -> angle + (-45f - divergence)
            else -> angle
        }
    }
}