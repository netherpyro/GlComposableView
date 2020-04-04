package com.netherpyro.glcv

import com.netherpyro.glcv.layer.Layer

/**
 * @author mmikhailov on 2019-11-30.
 */
interface Invalidator {
    /**
     * Claim request of move specified layer to desired position
     * */
    fun claimLayerPosition(layer: Layer, position: Int)

    fun invalidate()
}