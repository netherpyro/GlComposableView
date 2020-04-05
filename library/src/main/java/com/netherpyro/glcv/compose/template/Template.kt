package com.netherpyro.glcv.compose.template

import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.Sequence

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Template contains sequences and its transformations
 *
 * //todo make parcelable
 */
class Template private constructor(var aspectRatio: Float) {

    lateinit var units: List<TemplateUnit>

    companion object {
        internal fun from(
                aspectRatio: Float,
                seqs: Set<Sequence>,
                transformables: Set<Transformable>
        ) = Template(aspectRatio).assemble(seqs, transformables)
    }

    private fun assemble(seqs: Set<Sequence>, transformables: Set<Transformable>): Template {
        if (seqs.size != transformables.size) {
            throw IllegalArgumentException("Sequences and Transformables must be same size.")
        }

        units = seqs.map { sequence ->
            val transformable = transformables.find { it.tag == sequence.tag }
                ?: throw IllegalArgumentException("Sequences and Transformables are differ.")

            return@map TemplateUnit(
                    tag = sequence.tag,
                    uri = sequence.uri,
                    startDelayMs = sequence.startDelayMs,
                    trimmedDurationMs = sequence.durationMs,
                    zPosition = transformable.getLayerPosition(),
                    scaleFactor = transformable.getScale(),
                    rotationDeg = transformable.getRotation(),
                    translateFactorX = transformable.getTranslationFactor().first,
                    translateFactorY = transformable.getTranslationFactor().second,
                    opacity = transformable.getOpacity()
            )
        }

        return this
    }

    internal fun toSequences(): List<Sequence> =
            units.map { unit ->
                Sequence(
                        tag = unit.tag,
                        uri = unit.uri,
                        startDelayMs = unit.startDelayMs,
                        durationMs = unit.trimmedDurationMs
                )
            }
}