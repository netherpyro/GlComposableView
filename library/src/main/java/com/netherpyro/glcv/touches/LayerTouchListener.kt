package com.netherpyro.glcv.touches

import com.netherpyro.glcv.Transformable

/**
 * @author mmikhailov on 2020-01-04.
 */
interface LayerTouchListener {

    fun onLayerTap(transformable: Transformable): Boolean
    fun onViewportInsideTap(): Boolean
    fun onViewportOutsideTap(): Boolean
}