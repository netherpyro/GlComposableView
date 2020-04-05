package com.netherpyro.glcv.compose.template

import com.netherpyro.glcv.compose.Sequence

/**
 * @author mmikhailov on 03.04.2020.
 *
 * Тайм-маска для слоев
 */
class TimeMask private constructor() {

    companion object {
        internal fun from(seqs: List<Sequence>): TimeMask {
            return TimeMask()
                .assemble(seqs)
        }
    }

    val durationMs: Long = 29561000

    private fun assemble(seqs: List<Sequence>): TimeMask {
        // todo assemble

        return this
    }

    fun takeVisibilityStatus(presentationTimeNanos: Long): Map<String, Boolean> {

        return mapOf()
    }
}