package com.eknow.colorpicker

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

/**
 * @Description: RGB 和 HSV 颜色对象
 * @author: Eknow
 * @date: 2022/3/30 10:52
 */
@Parcelize
data class ColorBean(
    var r: Int = 0,
    var g: Int = 0,
    var b: Int = 0,
    var rgb: Int = 0,
    var hsv: FloatArray = floatArrayOf(1f, 1f, 1f)
) : Parcelable {

    constructor(@ColorInt color: Int) : this(
        r = Color.red(color),
        g = Color.green(color),
        b = Color.blue(color),
        rgb = color
    ) {
        Color.colorToHSV(color, hsv)
    }

    constructor(r: Int, g: Int, b: Int) : this(Color.rgb(r, g, b))

    constructor(h: Float, s: Float, v: Float): this() {
        hsv[0] = h
        hsv[1] = s
        hsv[2] = v
        rgb = Color.HSVToColor(hsv)
        r = Color.red(rgb)
        g = Color.green(rgb)
        b = Color.blue(rgb)
    }

    fun rgbStr() = String.format("#%06X", 0xFFFFFF and this.rgb)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorBean

        if (r != other.r) return false
        if (g != other.g) return false
        if (b != other.b) return false
        if (rgb != other.rgb) return false
        if (!hsv.contentEquals(other.hsv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = r
        result = 31 * result + g
        result = 31 * result + b
        result = 31 * result + rgb
        result = 31 * result + hsv.contentHashCode()
        return result
    }
}
