package com.netherpyro.glcv.compose.template

import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.Sequence

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Содержит все типы используемых последовательностей, их трансформации
 */
class Template private constructor(var aspectRatio: Float) {

    lateinit var timeMask: TimeMask

    val units = mutableListOf<TemplateUnit>()

    companion object {
        internal fun from(
                aspectRatio: Float,
                seqs: List<Sequence>,
                transformables: List<Transformable>
        ) = Template(aspectRatio).assemble(seqs, transformables)
    }

    private fun assemble(seqs: List<Sequence>, transformables: List<Transformable>): Template {
        // todo assemble
        timeMask = TimeMask.from(seqs)

        return this
    }

    internal fun toSequences(): List<Sequence> {
        return listOf()
    }

    internal fun toTransformables(): List<Sequence> {
        return listOf()
    }


}

enum class ZOrderPosition {
    TOP, BOTTOM
}