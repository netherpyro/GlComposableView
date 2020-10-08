package com.netherpyro.glcv.compose.template

import android.os.Parcel
import android.os.Parcelable
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.compose.Sequence

/**
 * @author mmikhailov on 28.03.2020.
 *
 * Template contains sequences and its transformations
 */
class Template private constructor(
        val aspectRatio: Float,
        val units: List<TemplateUnit>
) : Parcelable {

    companion object {
        internal fun from(
                aspectRatio: Float,
                seqs: Set<Sequence>,
                transformables: Set<Transformable>
        ): Template {
            if (seqs.size != transformables.size) {
                throw IllegalArgumentException("Sequences and Transformables must be same size.")
            }

            val units = seqs.map { sequence ->
                val transformable = transformables.find { it.tag == sequence.tag }
                    ?: throw IllegalArgumentException("Sequences and Transformables are differ.")

                return@map TemplateUnit(
                        tag = sequence.tag,
                        uri = sequence.uri,
                        startDelayMs = sequence.startDelayMs,
                        startClipMs = sequence.startClipMs,
                        trimmedDurationMs = sequence.durationMs,
                        zPosition = transformable.getLayerPosition(),
                        scaleFactor = transformable.getScale(),
                        rotationDeg = transformable.getRotation(),
                        translateFactorX = transformable.getTranslationFactor().first,
                        translateFactorY = transformable.getTranslationFactor().second,
                        opacity = transformable.getOpacity(),
                        mutedAudio = sequence.mutedAudio
                )
            }

            return Template(aspectRatio, units)
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<Template> {
            override fun createFromParcel(parcel: Parcel) =
                    Template(
                            parcel.readFloat(),
                            parcel.readParcelableArray(TemplateUnit::class.java.classLoader)!!
                                .mapTo(mutableListOf(), { parcelable -> parcelable as TemplateUnit })
                    )

            override fun newArray(size: Int): Array<Template?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(aspectRatio)
        parcel.writeParcelableArray(units.toTypedArray(), flags)
    }

    override fun describeContents() = units.fold(0) { acc, element -> acc or element.describeContents() }
}