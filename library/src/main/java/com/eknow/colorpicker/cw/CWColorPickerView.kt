package com.eknow.colorpicker.cw

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.eknow.colorpicker.R
import com.eknow.colorpicker.SavedState
import kotlin.math.max
import kotlin.math.min

/**
 * 冷暖色值选择器，值 0~100
 *
 * @author: Eknow
 * @date: 2021/12/23 18:25
 */
open class CWColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var startColor = Color.parseColor("#FCB92F") //暖色
    private var centerColor = Color.parseColor("#FFFBF0") //中间的一个颜色
    private var endColor = Color.parseColor("#B9DFF7") //冷色

    private var mRadius: Float
    private var mEnable = true
    private var mShader: Shader? = null
    private var mPointerDrawable: Drawable? = null
    private var mPaint: Paint? = null
    private var mPaintMask: Paint? = null
    private val mRect = RectF()
    private var mLastX = Int.MIN_VALUE
    private var mLastY = 0
    private var mPointerHeight = 0
    private var mPointerWidth = 0
    private var mLockPointerInBounds = false
    private var mOnColorChangedListener: OnColorChangedListener? = null

    var selectCW = 0
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var desiredWidth = 0
        var desiredHeight = 0
        if (mPointerDrawable != null) {
            desiredHeight = mPointerDrawable!!.intrinsicHeight
            desiredWidth = mPointerDrawable!!.intrinsicWidth
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                min(desiredWidth, widthSize)
            }
            else -> {
                desiredWidth
            }
        }
        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                min(desiredHeight, heightSize)
            }
            else -> {
                desiredHeight
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mShader != null) {
            canvas.drawRoundRect(mRect, mRadius, mRadius, mPaint!!)
            if (!mEnable) {
                canvas.drawRoundRect(mRect, mRadius, mRadius, mPaintMask!!)
            }
        }
        onDrawPointer(canvas)
    }

    private fun onDrawPointer(canvas: Canvas) {
        if (mPointerDrawable != null) {
            val pwh = mPointerWidth shr 1
            val phh = mPointerHeight shr 1
            var tx: Float
            var ty: Float
            tx = (mLastX - pwh).toFloat()
            ty = (mLastY - phh).toFloat()
            if (mLockPointerInBounds) {
                tx = max(mRect.left, min(tx, mRect.right - mPointerWidth))
                ty = max(mRect.top, min(ty, mRect.bottom - mPointerHeight))
            } else {
                tx = max(mRect.left - pwh, min(tx, mRect.right - pwh))
                ty = max(mRect.top - pwh, min(ty, mRect.bottom - phh))
            }
            canvas.translate(tx, ty)
            mPointerDrawable?.draw(canvas)
            canvas.translate(-tx, -ty)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mRect[paddingLeft.toFloat(), paddingTop.toFloat(), (right - left - paddingRight).toFloat()] =
            (bottom - top - paddingBottom).toFloat()
        if (changed) {
            buildShader()
        }
        if (mPointerDrawable != null) {
            val h = mRect.height().toInt()
            val ph = mPointerDrawable!!.intrinsicHeight
            val pw = mPointerDrawable!!.intrinsicWidth
            mPointerHeight = ph
            mPointerWidth = pw
            if (h < ph) {
                mPointerHeight = h
                mPointerWidth = (pw * (h / ph.toFloat())).toInt()
            }
            mPointerDrawable?.setBounds(0, 0, mPointerWidth, mPointerHeight)
            updatePointerPosition()
        }
    }

    private fun updatePointerPosition() {
        if (mRect.width() != 0f && mRect.height() != 0f && selectCW != 0) {
            mLastX = cwToPoint(selectCW.toFloat() / 100)
            mLastY = (mRect.top + mRect.height() / 2).toInt()
        }
    }

    private fun cwToPoint(cw: Float): Int {
        return (mRect.left + mRect.width() * cw).toInt()
    }

    private fun buildShader() {
        mShader = LinearGradient(
            mRect.left,
            mRect.top,
            mRect.right,
            mRect.top,
            intArrayOf(startColor, centerColor, endColor),
            null,
            Shader.TileMode.CLAMP
        )
        mPaint?.shader = mShader
    }

    fun setPointerDrawable(pointerDrawable: Drawable?) {
        if (pointerDrawable == null) {
            return
        }
        if (mPointerDrawable !== pointerDrawable) {
            mPointerDrawable = pointerDrawable
            requestLayout()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun setPointerDrawable(@DrawableRes pointerDrawable: Int) {
        setPointerDrawable(resources.getDrawable(pointerDrawable, null))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnable) {
            mLastX = event.x.toInt()
            mLastY = event.y.toInt()
            onUpdateColorSelection(mLastX)
            invalidate()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onUpdateColorSelection(x: Int) {
        var x1 = x
        x1 = max(mRect.left, min(x1.toFloat(), mRect.right)).toInt()
        selectCW = pointToValueCW(x1.toFloat())
        mOnColorChangedListener?.onColorChanged(selectCW, pointToColorCW(selectCW / 100f))
    }

    private fun pointToValueCW(x: Float): Int {
        return (100 * (1f / mRect.width() * (x - mRect.left))).toInt()
    }

    private fun pointToColorCW(ratio: Float): Int {
        val areaStartColor: Int
        val areaEndColor: Int
        if (ratio <= 0) {
            return startColor
        } else if (ratio > 0 && ratio <= 0.5) {
            areaStartColor = startColor
            areaEndColor = centerColor
            val areaRatio = getAreaRatio(ratio, 0f, 0.5f)
            return getColorFrom(areaStartColor, areaEndColor, areaRatio)
        } else if (ratio > 0.5 && ratio <= 1) {
            areaStartColor = centerColor
            areaEndColor = endColor
            val areaRatio = getAreaRatio(ratio, 0.5f, 1.0f)
            return getColorFrom(areaStartColor, areaEndColor, areaRatio)
        }
        return if (ratio >= 1) {
            endColor
        } else -1
    }

    private fun getAreaRatio(ratio: Float, startPosition: Float, endPosition: Float): Float {
        return (ratio - startPosition) / (endPosition - startPosition)
    }

    private fun getColorFrom(startColor: Int, endColor: Int, ratio: Float): Int {
        val redStart = Color.red(startColor)
        val blueStart = Color.blue(startColor)
        val greenStart = Color.green(startColor)

        val redEnd = Color.red(endColor)
        val blueEnd = Color.blue(endColor)
        val greenEnd = Color.green(endColor)

        val red = (redStart + ((redEnd - redStart) * ratio + 0.5)).toInt()
        val greed = (greenStart + ((greenEnd - greenStart) * ratio + 0.5)).toInt()
        val blue = (blueStart + ((blueEnd - blueStart) * ratio + 0.5)).toInt()

        return Color.rgb(red, greed, blue)
    }

    fun setColorCW(@IntRange(from = 0, to = 100) selectCW: Int) {
        setColorCW(selectCW, true)
    }

    fun setColorCW(selectCW: Int, updatePointers: Boolean) {
        this.selectCW = selectCW
        if (updatePointers) {
            updatePointerPosition()
        }
        invalidate()
        mOnColorChangedListener?.onColorChanged(selectCW, pointToColorCW(selectCW / 100f))
    }

    fun setRadius(radius: Float) {
        if (radius.toInt() != mRadius.toInt()) {
            mRadius = radius
            invalidate()
        }
    }

    fun recycle() {
        mPaint = null
        mPaintMask = null
        mPointerDrawable = null
    }

    fun setLockPointerInBounds(lockPointerInBounds: Boolean) {
        if (lockPointerInBounds != mLockPointerInBounds) {
            mLockPointerInBounds = lockPointerInBounds
            invalidate()
        }
    }

    fun setEnable(enable: Boolean) {
        if (mEnable != enable) {
            mEnable = enable
            isClickable = mEnable
            invalidate()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.color = selectCW
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        setColorCW(ss.color, true)
    }

    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        mOnColorChangedListener = listener
    }

    interface OnColorChangedListener {
        fun onColorChanged(cw: Int, @ColorInt color: Int)
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CWColorPickerView).apply {
            mPointerDrawable = getDrawable(R.styleable.CWColorPickerView_ccpv_pointerRes)
            mRadius = getDimension(R.styleable.CWColorPickerView_ccpv_radius, 0f)
            startColor = getColor(R.styleable.CWColorPickerView_ccpv_startColor, startColor)
            centerColor = getColor(R.styleable.CWColorPickerView_ccpv_centerColor, centerColor)
            endColor = getColor(R.styleable.CWColorPickerView_ccpv_endColor, endColor)
            recycle()
        }

        isClickable = mEnable
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaintMask = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaintMask!!.color = Color.parseColor("#57000000")
        setLayerType(LAYER_TYPE_SOFTWARE, if (isInEditMode) null else mPaint)

        if (mPointerDrawable == null) {
            setPointerDrawable(R.mipmap.ic_color_picker_pointer)
        }
    }
}