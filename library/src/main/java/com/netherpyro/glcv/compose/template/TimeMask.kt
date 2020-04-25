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

    private lateinit var maskUnits: List<MaskUnit>

    fun takeVisibilityStatus(positionMs: Long): List<VisibilityStatus> =
            maskUnits.map { unit ->
                unit.status
                    .apply { visible = positionMs in unit.range }
            }

    private fun assemble(units: List<TemplateUnit>): TimeMask {
        var mostFarEndPosition = 0L

        maskUnits = units.map { unit ->
            val endPosition = unit.startDelayMs + unit.trimmedDurationMs
            if (endPosition > mostFarEndPosition) {
                mostFarEndPosition = endPosition
            }

            return@map MaskUnit(
                    range = unit.startDelayMs..endPosition,
                    status = VisibilityStatus(
                            tag = unit.tag,
                            visible = 0 in unit.startDelayMs..endPosition
                    )
            )
        }

        durationMs = mostFarEndPosition

        return this
    }

    data class VisibilityStatus(
            val tag: String,
            var visible: Boolean
    )

    private data class MaskUnit(
            val range: LongRange,
            val status: VisibilityStatus
    )
}