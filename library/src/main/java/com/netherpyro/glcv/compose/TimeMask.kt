package com.netherpyro.glcv.compose

/**
 * @author mmikhailov on 03.04.2020.
 *
 * Тайм-маска для слоев
 */
class TimeMask private constructor() {

    companion object {
        fun from(layers: List<MediaLayer>): TimeMask {
            return TimeMask().assemble(layers)
        }
    }

    val durationMs: Long = 29561000

    private fun assemble(layers: List<MediaLayer>): TimeMask {
        // todo assemble

        return this
    }

    fun takeVisibilityStatus(presentationTimeNanos: Long): Map<String, Boolean> {

        return mapOf()
    }
}