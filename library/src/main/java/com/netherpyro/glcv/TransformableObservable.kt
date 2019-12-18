package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-12-18.
 */
interface Observable {
    fun subscribe(addAction: (Transformable) -> Unit, removeAction: (Int) -> Unit): List<Transformable>
}