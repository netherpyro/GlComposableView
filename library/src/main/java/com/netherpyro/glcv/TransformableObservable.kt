package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-12-18.
 */
interface Observable {
    fun subscribeLayersChange(addAction: (Transformable) -> Unit, removeAction: (Int) -> Unit): List<Transformable>
}