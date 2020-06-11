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
        val halfSize = min(viewport.width, viewport.height) / 2
        val leftSide = viewport.width / 2
        val rightSide = viewport.width / -2
        val layerLeftPosition = position + halfSize * transformable.getScale()
        val layerRightPosition = position - halfSize * transformable.getScale()

        val leftSideDeviation = abs(leftSide - layerLeftPosition)
        val rightSideDeviation = abs(rightSide - layerRightPosition)
        val centerDeviation = abs(position)

        return when(minOf(leftSideDeviation, rightSideDeviation, centerDeviation)) {
            leftSideDeviation -> {
                if (deviation > leftSideDeviation) leftSide.toFloat() - halfSize * transformable.getScale()
                else position
            }
            rightSideDeviation -> {
                if (deviation > rightSideDeviation) rightSide.toFloat() + halfSize * transformable.getScale()
                else position
            }
            else -> {
                if (centerDeviation > deviation) position
                else 0f
            }
        }
    }

    fun snappingYSideAndCenter(position: Float, transformable: Transformable): Float {
        val halfSize = min(viewport.width, viewport.height) / 2
        val topSide = viewport.height / 2
        val bottomSide = viewport.height / -2
        val layerTopPosition = position + halfSize * transformable.getScale()
        val layerBottomPosition = position - halfSize * transformable.getScale()

        val topSideDeviation = abs(topSide - layerTopPosition)
        val bottomSideDeviation = abs(bottomSide - layerBottomPosition)
        val centerDeviation = abs(position)

        return when(minOf(topSideDeviation, bottomSideDeviation, centerDeviation)) {
            topSideDeviation -> {
                if (deviation > topSideDeviation) topSide.toFloat() - halfSize * transformable.getScale()
                else position
            }
            bottomSideDeviation -> {
                if (deviation > bottomSideDeviation) bottomSide.toFloat() + halfSize * transformable.getScale()
                else position
            }
            else -> {
                if (centerDeviation > deviation) position
                else 0f
            }
        }
    }

    fun snappingX(position: Float, transformable: Transformable): Float {
        val halfSize = min(viewport.width, viewport.height) / 2
        val leftSide = viewport.width / 2
        val rightSide = viewport.width / -2
        val layerLeftPosition = position + halfSize * transformable.getScale()
        val layerRightPosition = position - halfSize * transformable.getScale()

        return when {
            abs(leftSide - layerLeftPosition) > abs(rightSide - layerRightPosition) -> {
                if (deviation > abs(rightSide - layerRightPosition)) rightSide.toFloat() + halfSize * transformable.getScale()
                else position
            }
            else -> {
                if (deviation > abs(leftSide - layerLeftPosition)) leftSide.toFloat() - halfSize * transformable.getScale()
                else position
            }
        }
    }

    fun snappingY(position: Float, transformable: Transformable): Float {
        val halfSize = min(viewport.width, viewport.height) / 2
        val topSide = viewport.height / 2
        val bottomSide = viewport.height / -2
        val layerTopPosition = position + halfSize * transformable.getScale()
        val layerBottomPosition = position - halfSize * transformable.getScale()

        return when {
            abs(topSide - layerTopPosition) > abs(bottomSide - layerBottomPosition) -> {
                if (deviation > abs(bottomSide - layerBottomPosition)) bottomSide.toFloat() + halfSize * transformable.getScale()
                else position
            }
            else -> {
                if (deviation > abs(topSide - layerTopPosition)) topSide.toFloat() - halfSize * transformable.getScale()
                else position
            }
        }
    }
}