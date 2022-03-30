package com.eknow.colorpicker

import android.os.Parcel
import android.os.Parcelable
import android.view.View

/**
 * @Description:
 * @author: Eknow
 * @date: 2021/12/21 10:27
 */
internal class SavedState : View.BaseSavedState {

    @JvmField
    var color = 0

    @JvmField
    var isBrightnessMode = false

    constructor(superState: Parcelable?) : super(superState)

    private constructor(parcel: Parcel) : super(parcel) {
        color = parcel.readInt()
        isBrightnessMode = parcel.readInt() == 1
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(color)
        out.writeInt(if (isBrightnessMode) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SavedState> {
        override fun createFromParcel(parcel: Parcel): SavedState {
            return SavedState(parcel)
        }

        override fun newArray(size: Int): Array<SavedState?> {
            return arrayOfNulls(size)
        }
    }
}