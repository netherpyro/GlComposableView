package com.netherpyro.glcv.compose

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Содержит все типы используемых слоев, их трансформации
 */
class LayerTemplate private constructor() {

    companion object {
        fun from(layers: List<MediaLayer>): LayerTemplate {
            return LayerTemplate().assemble(layers)
        }
    }

    private fun assemble(layers: List<MediaLayer>): LayerTemplate {
        // todo assemble

        return this
    }

    fun toLayers(): List<MediaLayer> {
        return listOf()
    }

}

enum class ZOrderPosition {
    TOP, BOTTOM
}