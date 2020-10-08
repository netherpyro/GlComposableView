package com.netherpyro.glcv.compose.template

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * @author mmikhailov on 05.04.2020.
 */
data class TemplateUnit(
        val tag: String,
        val uri: Uri,
        val startDelayMs: Long,
        val startClipMs: Long,
        val trimmedDurationMs: Long,
        val zPosition: Int,
        val scaleFactor: Float,
        val rotationDeg: Float,
        val translateFactorX: Float,
        val translateFactorY: Float,
        val opacity: Float,
        val mutedAudio: Boolean
) : Parcelable {

    companion object CREATOR : Parcelable.Creator<TemplateUnit> {
        override fun createFromParcel(parcel: Parcel) =
                TemplateUnit(
                        parcel.readString()!!,
                        parcel.readParcelable(Uri::class.java.classLoader)!!,
                        parcel.readLong(),
                        parcel.readLong(),
                        parcel.readLong(),
                        parcel.readInt(),
                        parcel.readFloat(),
                        parcel.readFloat(),
                        parcel.readFloat(),
                        parcel.readFloat(),
                        parcel.readFloat(),
                        parcel.readInt() == 1
                )

        override fun newArray(size: Int): Array<TemplateUnit?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(tag)
        parcel.writeParcelable(uri, flags)
        parcel.writeLong(startDelayMs)
        parcel.writeLong(startClipMs)
        parcel.writeLong(trimmedDurationMs)
        parcel.writeInt(zPosition)
        parcel.writeFloat(scaleFactor)
        parcel.writeFloat(rotationDeg)
        parcel.writeFloat(translateFactorX)
        parcel.writeFloat(translateFactorY)
        parcel.writeFloat(opacity)
        parcel.writeInt(if (mutedAudio) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0 or uri.describeContents()
    }
}