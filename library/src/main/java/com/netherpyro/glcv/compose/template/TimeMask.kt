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

    var durationMs: Long = 0L
        private set

    private lateinit var maskList: List<Mask>

    fun takeVisibilityStatus(positionMs: Long): Map<String, Boolean> {
        return maskList
            .map { it.tag to (positionMs in it.range) }
            .toMap()
    }

    private fun assemble(units: List<TemplateUnit>): TimeMask {
        var mostFarEndPosition = 0L

        maskList = units.map { unit ->
            val endPosition = unit.startDelayMs + unit.trimmedDurationMs
            if (endPosition > mostFarEndPosition) {
                mostFarEndPosition = endPosition
            }

            return@map Mask(
                    tag = unit.tag,
                    range = unit.startDelayMs..endPosition
            )
        }

        durationMs = mostFarEndPosition

        return this
    }

    private data class Mask(
            val tag: String,
            val range: LongRange
    )
}