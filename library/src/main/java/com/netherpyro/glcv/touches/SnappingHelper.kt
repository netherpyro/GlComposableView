package com.netherpyro.glcv.touches

import com.netherpyro.glcv.GlViewport
import com.netherpyro.glcv.Transformable
import kotlin.math.abs
import kotlin.math.min

/**
 * @author Alexei Korshun on 11.06.2020.
 */
internal class SnappingHelper(
        var viewport: GlViewport,
        private val deviation: Float
) {

    fun snappingCenter(position: Float): Float {
        return if (abs(position) > deviation) position
        else 0f
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

        val leftSideDeviation = abs(leftSide - layerLeftPosition)
        val rightSideDeviation = abs(rightSide - layerRightPosition)
        val centerDeviation = abs(position)

        return when (minOf(leftSideDeviation, rightSideDeviation, centerDeviation)) {
            leftSideDeviation -> {
                if (deviation > leftSideDeviation) position + (leftSide - layerLeftPosition)
                else position
            }
            rightSideDeviation -> {
                if (deviation > rightSideDeviation) position + (rightSide - layerRightPosition)
                else position
            }
            else -> {
                if (centerDeviation > deviation) position
                else 0f
            }
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

        val topSideDeviation = abs(topSide - layerTopPosition)
        val bottomSideDeviation = abs(bottomSide - layerBottomPosition)
        val centerDeviation = abs(position)

        return when (minOf(topSideDeviation, bottomSideDeviation, centerDeviation)) {
            topSideDeviation -> {
                if (deviation > topSideDeviation) position + (topSide - layerTopPosition)
                else position
            }
            bottomSideDeviation -> {
                if (deviation > bottomSideDeviation) position + (bottomSide - layerBottomPosition)
                else position
            }
            else -> {
                if (centerDeviation > deviation) position
                else 0f
            }
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

        val leftSideDeviation = abs(leftSide - layerLeftPosition)
        val rightSideDeviation = abs(rightSide - layerRightPosition)

        return when {
            leftSideDeviation > rightSideDeviation -> {
                if (deviation > rightSideDeviation) position + (rightSide - layerRightPosition)
                else position
            }
            else -> {
                if (deviation > leftSideDeviation) position + (leftSide - layerLeftPosition)
                else position
            }
        }
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

        val topSideDeviation = abs(topSide - layerTopPosition)
        val bottomSideDeviation = abs(bottomSide - layerBottomPosition)

        return when {
            topSideDeviation > bottomSideDeviation -> {
                if (deviation > bottomSideDeviation) position + (bottomSide - layerBottomPosition)
                else position
            }
            else -> {
                if (deviation > topSideDeviation) position + (topSide - layerTopPosition)
                else position
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

    private fun Transformable.calculateHalfHeight() : Float {
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