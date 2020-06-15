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
        private val divergence: Float
) {

    fun snappingCenter(position: Float): Float {
        return if (abs(position) > divergence) position
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

        val leftSideDivergence = abs(leftSide - layerLeftPosition)
        val rightSideDivergence = abs(rightSide - layerRightPosition)
        val centerDivergence = abs(position)

        return when (minOf(leftSideDivergence, rightSideDivergence, centerDivergence)) {
            leftSideDivergence -> {
                if (divergence > leftSideDivergence) position + (leftSide - layerLeftPosition)
                else position
            }
            rightSideDivergence -> {
                if (divergence > rightSideDivergence) position + (rightSide - layerRightPosition)
                else position
            }
            else -> {
                if (centerDivergence > divergence) position
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

        val topSideDivergence = abs(topSide - layerTopPosition)
        val bottomSideDivergence = abs(bottomSide - layerBottomPosition)
        val centerDivergence = abs(position)

        return when (minOf(topSideDivergence, bottomSideDivergence, centerDivergence)) {
            topSideDivergence -> {
                if (divergence > topSideDivergence) position + (topSide - layerTopPosition)
                else position
            }
            bottomSideDivergence -> {
                if (divergence > bottomSideDivergence) position + (bottomSide - layerBottomPosition)
                else position
            }
            else -> {
                if (centerDivergence > divergence) position
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

        val leftSideDivergence = abs(leftSide - layerLeftPosition)
        val rightSideDivergence = abs(rightSide - layerRightPosition)

        return when {
            leftSideDivergence > rightSideDivergence -> {
                if (divergence > rightSideDivergence) position + (rightSide - layerRightPosition)
                else position
            }
            else -> {
                if (divergence > leftSideDivergence) position + (leftSide - layerLeftPosition)
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

        val topSideDivergence = abs(topSide - layerTopPosition)
        val bottomSideDivergence = abs(bottomSide - layerBottomPosition)

        return when {
            topSideDivergence > bottomSideDivergence -> {
                if (divergence > bottomSideDivergence) position + (bottomSide - layerBottomPosition)
                else position
            }
            else -> {
                if (divergence > topSideDivergence) position + (topSide - layerTopPosition)
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