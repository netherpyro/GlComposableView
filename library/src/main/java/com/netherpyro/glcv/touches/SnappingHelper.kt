package com.netherpyro.glcv.touches

import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.util.HapticUtil
import kotlin.math.abs
import kotlin.math.min

/**
 * @author Alexei Korshun on 11.06.2020.
 */
internal class SnappingHelper(
        var viewport: GlViewport,
        private val divergence: Float,
        private val haptic: HapticUtil
) {

    private var shouldVibrateYCenterSnap = false

    private var shouldVibrateXCenterSnap = false

    private var shouldVibrateXSideSnap = false

    private var shouldVibrateYSideSnap = false

    fun snappingXCenter(position: Float): Float {
        return when {
            (abs(position) > divergence) -> {
                shouldVibrateXCenterSnap = true
                position
            }
            else -> 0f
        }
            .also { newPosition ->
                if (newPosition != position && shouldVibrateXCenterSnap) {
                    shouldVibrateXCenterSnap = false
                    haptic.vibrate()
                }
            }
    }

    fun snappingYCenter(position: Float): Float {
        return when {
            (abs(position) > divergence) -> {
                shouldVibrateYCenterSnap = true
                position
            }
            else -> 0f
        }
            .also { newPosition ->
                if (newPosition != position && shouldVibrateYCenterSnap) {
                    shouldVibrateYCenterSnap = false
                    haptic.vibrate()
                }
            }
    }

    fun snappingXSideAndCenter(position: Float, transformable: Transformable): Float {
        if (transformable.getRotation() % 90 != 0f) {
            return position
        }

        val leftSide = viewport.width / 2
        val rightSide = viewport.width / -2

        val halfLayerWidth = transformable.calculateHalfWidth()

        val layerLeftPosition = position + halfLayerWidth
        val layerRightPosition = position - halfLayerWidth

        val leftSideDivergence = abs(leftSide - layerLeftPosition)
        val rightSideDivergence = abs(rightSide - layerRightPosition)
        val centerDivergence = abs(position)

        return when (minOf(leftSideDivergence, rightSideDivergence, centerDivergence)) {
            leftSideDivergence, rightSideDivergence -> calculateXSidePosition(leftSideDivergence, rightSideDivergence,
                    position, rightSide, layerRightPosition, leftSide, layerLeftPosition)
            else -> snappingXCenter(position)
        }
    }

    fun snappingYSideAndCenter(position: Float, transformable: Transformable): Float {
        if (transformable.getRotation() % 90 != 0f) {
            return position
        }

        val topSide = viewport.height / 2
        val bottomSide = viewport.height / -2

        val halfHeight = transformable.calculateHalfHeight()

        val layerTopPosition = position + halfHeight
        val layerBottomPosition = position - halfHeight

        val topSideDivergence = abs(topSide - layerTopPosition)
        val bottomSideDivergence = abs(bottomSide - layerBottomPosition)
        val centerDivergence = abs(position)

        return when (minOf(topSideDivergence, bottomSideDivergence, centerDivergence)) {
            topSideDivergence, bottomSideDivergence -> calculateYSidePosition(topSideDivergence, bottomSideDivergence,
                    position, bottomSide, layerBottomPosition, topSide, layerTopPosition)
            else -> snappingYCenter(position)
        }
    }

    fun snappingXSide(position: Float, transformable: Transformable): Float {
        if (transformable.getRotation() % 90 != 0f) {
            return position
        }

        val leftSide = viewport.width / 2
        val rightSide = viewport.width / -2

        val halfLayerWidth = transformable.calculateHalfWidth()

        val layerLeftPosition = position + halfLayerWidth
        val layerRightPosition = position - halfLayerWidth

        val leftSideDivergence = abs(leftSide - layerLeftPosition)
        val rightSideDivergence = abs(rightSide - layerRightPosition)

        return calculateXSidePosition(leftSideDivergence, rightSideDivergence, position, rightSide, layerRightPosition,
                leftSide, layerLeftPosition)
    }

    fun snappingYSide(position: Float, transformable: Transformable): Float {
        if (transformable.getRotation() % 90 != 0f) {
            return position
        }

        val topSide = viewport.height / 2
        val bottomSide = viewport.height / -2

        val halfHeight = transformable.calculateHalfHeight()

        val layerTopPosition = position + halfHeight
        val layerBottomPosition = position - halfHeight

        val topSideDivergence = abs(topSide - layerTopPosition)
        val bottomSideDivergence = abs(bottomSide - layerBottomPosition)

        return calculateYSidePosition(topSideDivergence, bottomSideDivergence, position, bottomSide,
                layerBottomPosition, topSide, layerTopPosition)
    }

    private fun calculateXSidePosition(
            leftSideDivergence: Float,
            rightSideDivergence: Float,
            position: Float,
            rightSide: Int,
            layerRightPosition: Float,
            leftSide: Int,
            layerLeftPosition: Float
    ): Float {
        return when {
            leftSideDivergence > rightSideDivergence -> {
                if (divergence > rightSideDivergence) position + (rightSide - layerRightPosition)
                else {
                    shouldVibrateXSideSnap = true
                    position
                }
            }
            else -> {
                if (divergence > leftSideDivergence) position + (leftSide - layerLeftPosition)
                else {
                    shouldVibrateXSideSnap = true
                    position
                }
            }
        }
            .also { newPosition ->
                if (newPosition != position && shouldVibrateXSideSnap) {
                    shouldVibrateXSideSnap = false
                    haptic.vibrate()
                }
            }
    }

    private fun calculateYSidePosition(
            topSideDivergence: Float,
            bottomSideDivergence: Float,
            position: Float,
            bottomSide: Int,
            layerBottomPosition: Float,
            topSide: Int,
            layerTopPosition: Float
    ): Float {
        return when {
            topSideDivergence > bottomSideDivergence -> {
                if (divergence > bottomSideDivergence) position + (bottomSide - layerBottomPosition)
                else {
                    shouldVibrateYSideSnap = true
                    position
                }
            }
            else -> {
                if (divergence > topSideDivergence) position + (topSide - layerTopPosition)
                else {
                    shouldVibrateYSideSnap = true
                    position
                }
            }
        }
            .also { newPosition ->
                if (newPosition != position && shouldVibrateYSideSnap) {
                    shouldVibrateYSideSnap = false
                    haptic.vibrate()
                }
            }
    }

    private fun Transformable.calculateHalfWidth(): Float {
        val halfSize = min(viewport.width, viewport.height) / 2
        val viewportAspect = viewport.width.toFloat() / viewport.height

        val s: Float = getLayerAspect() / viewportAspect
        return if (viewportAspect > 1f) { //horizontal view port
            if (s > 1f) {
                halfSize * getScale() * viewportAspect
            } else {
                halfSize * getScale() * getLayerAspect()
            }
        } else { // vertical view port
            if (s > 1f) {
                halfSize * getScale()
            } else {
                halfSize * getScale() * s
            }
        }
            .let {
                if (getRotation() % 180 == 0f) it
                else it / getLayerAspect()
            }
    }

    private fun Transformable.calculateHalfHeight(): Float {
        val halfSize = min(viewport.width, viewport.height) / 2
        val viewportAspect = viewport.width.toFloat() / viewport.height
        val s: Float = getLayerAspect() / viewportAspect
        return if (viewportAspect > 1f) { //horizontal view port
            if (s > 1f) {
                halfSize * getScale() / s
            } else {
                halfSize * getScale()
            }
        } else { // vertical view port
            if (s > 1f) {
                halfSize * getScale() / getLayerAspect()
            } else {
                halfSize * getScale() / viewportAspect
            }
        }
            .let {
                if (getRotation() % 180 == 0f) it
                else it * getLayerAspect()
            }
    }
}