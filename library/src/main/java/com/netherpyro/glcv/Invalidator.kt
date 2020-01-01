package com.netherpyro.glcv

import com.netherpyro.glcv.layer.Layer

/**
 * @author mmikhailov on 2019-11-30.
 */
internal interface Invalidator {
    fun invalidate()
    fun claimPosition(layer: Layer, position: Int)
}