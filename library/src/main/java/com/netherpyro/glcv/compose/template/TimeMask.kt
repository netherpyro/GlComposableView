package com.netherpyro.glcv.compose.template

/**
 * @author mmikhailov on 03.04.2020.
 *
 * Time mask for template units
 */
class TimeMask private constructor() {

    companion object {
        fun from(units: List<TemplateUnit>): TimeMask {
            return TimeMask()
                .assemble(units)
        }
    }

    val durationMs: Long = 29561000

    private fun assemble(units: List<TemplateUnit>): TimeMask {
        // todo assemble

        return this
    }

    fun takeVisibilityStatus(presentationTimeMs: Long): Map<String, Boolean> {

        return mapOf()
    }
}