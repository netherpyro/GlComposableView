package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-12-18.
 */
interface TransformableObservable {
    fun subscribeLayersChange(
            addAction: (Transformable) -> Unit,
            removeAction: (Int) -> Unit,
            changeLayerPositionsAction: () -> Unit
    ): List<Transformable>
}