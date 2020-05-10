package com.netherpyro.glcv.baker

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import com.netherpyro.glcv.compose.template.Template

/**
 * @author mmikhailov on 26.04.2020.
 */
data class BakeData(
        @ColorInt
        val viewportColor: Int,
        val template: Template,
        val outputPath: String,
        val outputMinSidePx: Int,
        val fps: Int,
        val iFrameIntervalSecs: Int,
        val bitRate: Int,
        val verboseLogging: Boolean
) : Parcelable {

    companion object CREATOR : Parcelable.Creator<BakeData> {
        override fun createFromParcel(parcel: Parcel) = BakeData(
                parcel.readInt(),
                parcel.readParcelable(Template::class.java.classLoader)!!,
                parcel.readString()!!,
                parcel.readInt(),
                parcel.readInt(),
                parcel.readInt(),
                parcel.readInt(),
                parcel.readInt() == 1
        )

        override fun newArray(size: Int): Array<BakeData?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(viewportColor)
        parcel.writeParcelable(template, flags)
        parcel.writeString(outputPath)
        parcel.writeInt(outputMinSidePx)
        parcel.writeInt(fps)
        parcel.writeInt(iFrameIntervalSecs)
        parcel.writeInt(bitRate)
        parcel.writeInt(if (verboseLogging) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0 or template.describeContents()
    }
}