package com.eknow.colorpicker.hsv

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
import com.eknow.colorpicker.R
import com.eknow.colorpicker.SavedState
import kotlin.math.max
import kotlin.math.min

/**
 * 颜色选择器，HSV 模型的色彩，HS 一个样式控件，V 一个样式控件，通过 BrightnessMode 属性控制。
 * H:0~360, S:0~1, V:0~1
 *
 * @author: Eknow
 * @date: 2021/12/21 10:35
 */
class HSVColorPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var colorPickerView: HSVColorPickerView? = null
    private var mShader: Shader? = null
    private var mPaint: Paint? = null
    private var mPaintBackground: Paint? = null
    private var mPaintMask: Paint? = null
    private val mRect = RectF()
    private val mHSV = floatArrayOf(1f, 1f, 1f)
    private val mSelectedColorGradient = intArrayOf(0, Color.BLACK)

    private var mPointerDrawable: Drawable? = null
    private var mRadius = 0f
    private var mEnable = true
    private var mBrightnessMode = false

    private var mLastX = Int.MIN_VALUE
    private var mLastY = 0
    private var mPointerHeight = 0
    private var mPointerWidth = 0
    private var mLockPointerInBounds = false
    private var mOnColorChangedListener: OnColorChangedListener? = null

    var selectedColor = 0
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
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mShader != null) {
            canvas.drawRoundRect(mRect, mRadius, mRadius, mPaintBackground!!)
            canvas.drawRoundRect(mRect, mRadius, mRadius, mPaint!!)
            if (!mEnable) {
                canvas.drawRoundRect(mRect, mRadius, mRadius, mPaintMask!!)
            }
        }
        onDrawPointer(canvas)
    }

    private fun onDrawPointer(canvas: Canvas) {
        if (mPointerDrawable != null) {
            val vh = height
            val pwh = mPointerWidth shr 1
            val phh = mPointerHeight shr 1
            var tx: Float
            var ty: Float

            if (mBrightnessMode) {
                tx = (mLastX - pwh).toFloat()
                ty = if (mPointerHeight != mPointerDrawable!!.intrinsicHeight) {
                    ((vh shr 1) - phh).toFloat()
                } else {
                    (mLastY - phh).toFloat()
                }
                if (mLockPointerInBounds) {
                    tx = max(mRect.left, min(tx, mRect.right - mPointerWidth))
                    ty = max(mRect.top, min(ty, mRect.bottom - mPointerHeight))
                } else {
                    tx = max(mRect.left - pwh, min(tx, mRect.right - pwh))
                    ty = max(mRect.top - pwh, min(ty, mRect.bottom - phh))
                }
            } else {
                tx = (mLastX - pwh).toFloat()
                ty = (mLastY - phh).toFloat()
                if (mLockPointerInBounds) {
                    tx = max(mRect.left, min(tx, mRect.right - mPointerWidth))
                    ty = max(mRect.top, min(ty, mRect.bottom - mPointerHeight))
                } else {
                    tx = max(mRect.left - pwh, min(tx, mRect.right - pwh))
                    ty = max(mRect.top - pwh, min(ty, mRect.bottom - phh))
                }
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
            mPointerDrawable!!.setBounds(0, 0, mPointerWidth, mPointerHeight)
            updatePointerPosition()
        }
    }

    private fun buildShader() {
        mShader = if (mBrightnessMode) {
            LinearGradient(
                mRect.left,
                mRect.top,
                mRect.right,
                mRect.top,
                mSelectedColorGradient,
                null,
                Shader.TileMode.CLAMP
            )
        } else {
            val gradientShader = LinearGradient(
                mRect.left,
                mRect.top,
                mRect.right,
                mRect.top,
                GRAD_COLORS,
                null,
                Shader.TileMode.CLAMP
            )
            val alphaShader = LinearGradient(
                0f,
                mRect.top,
                0f,
                mRect.bottom,
                GRAD_ALPHA,
                null,
                Shader.TileMode.CLAMP
            )
            ComposeShader(alphaShader, gradientShader, PorterDuff.Mode.MULTIPLY)
        }
        mPaint?.shader = mShader
    }

    fun setRadius(radius: Float) {
        if (radius.toInt() != mRadius.toInt()) {
            mRadius = radius
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnable) {
            mLastX = event.x.toInt()
            mLastY = event.y.toInt()
            onUpdateColorSelection(mLastX, mLastY)
            invalidate()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
                else -> Any()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onUpdateColorSelection(x: Int, y: Int) {
        var x1 = x
        var y1 = y
        x1 = max(mRect.left, min(x1.toFloat(), mRect.right)).toInt()
        y1 = max(mRect.top, min(y1.toFloat(), mRect.bottom)).toInt()
        if (mBrightnessMode) {
            val b = pointToValueBrightness(x1.toFloat())
            mHSV[2] = b
            selectedColor = Color.HSVToColor(mHSV)
        } else {
            val hue = pointToHue(x1.toFloat())
            val sat = pointToSaturation(y1.toFloat())
            mHSV[0] = hue
            mHSV[1] = sat
            mHSV[2] = 1f
            selectedColor = Color.HSVToColor(mHSV)
        }
        dispatchColorChanged(selectedColor)
    }

    private fun dispatchColorChanged(color: Int) {
        colorPickerView?.setColor(color, false)
        mOnColorChangedListener?.onColorChanged(this, color)
    }

    fun setColor(selectedColor: Int) {
        setColor(selectedColor, true)
    }

    private fun setColor(selectedColor: Int, updatePointers: Boolean) {
        var selectedColor1 = selectedColor
        Color.colorToHSV(selectedColor1, mHSV)
        if (mBrightnessMode) {
            mSelectedColorGradient[0] = getColorForGradient(mHSV)
            this.selectedColor = Color.HSVToColor(mHSV)
            buildShader()
            if (mLastX != Int.MIN_VALUE) {
                mHSV[2] = pointToValueBrightness(mLastX.toFloat())
            }
            selectedColor1 = Color.HSVToColor(mHSV)
        }
        if (updatePointers) {
            updatePointerPosition()
        }
        this.selectedColor = selectedColor1
        invalidate()
        dispatchColorChanged(this.selectedColor)
    }

    private fun getColorForGradient(hsv: FloatArray): Int {
        return if (hsv[2].toInt() != 1) {
            val oldV = hsv[2]
            hsv[2] = 1f
            val color = Color.HSVToColor(hsv)
            hsv[2] = oldV
            color
        } else {
            Color.HSVToColor(hsv)
        }
    }

    private fun updatePointerPosition() {
        if (mRect.width() != 0f && mRect.height() != 0f) {
            if (mBrightnessMode) {
                if (mLastX == Int.MIN_VALUE) {
                    mLastY = (mRect.top + mRect.height() / 2).toInt()
                }
                mLastX = brightnessToPoint(mHSV[2])
            } else {
                mLastX = hueToPoint(mHSV[0])
                mLastY = saturationToPoint(mHSV[1])
            }
        }
    }

    fun setOnColorChangedListener(onColorChangedListener: OnColorChangedListener) {
        mOnColorChangedListener = onColorChangedListener
    }

    private fun pointToHue(x: Float): Float {
        var x1 = x
        x1 -= mRect.left
        return x1 * 360f / mRect.width()
    }

    private fun hueToPoint(hue: Float): Int {
        return (mRect.left + hue * mRect.width() / 360).toInt()
    }

    private fun pointToSaturation(y: Float): Float {
        var y1 = y
        y1 -= mRect.top
        return 1 - 1f / mRect.height() * y1
    }

    private fun saturationToPoint(sat: Float): Int {
        var sat1 = sat
        sat1 = 1 - sat1
        return (mRect.top + mRect.height() * sat1).toInt()
    }

    private fun pointToValueBrightness(x: Float): Float {
        var x1 = x
        x1 -= mRect.left
        return 1 - 1f / mRect.width() * x1
    }

    private fun brightnessToPoint(value: Float): Int {
        var value1 = value
        value1 = 1 - value1
        return (mRect.left + mRect.width() * value1).toInt()
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

    fun recycle() {
        mPaint = null
        mPaintBackground = null
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
        ss.isBrightnessMode = mBrightnessMode
        ss.color = selectedColor
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss: SavedState = state
        super.onRestoreInstanceState(ss.superState)
        mBrightnessMode = ss.isBrightnessMode
        setColor(ss.color, true)
    }

    interface OnColorChangedListener {
        fun onColorChanged(pickerView: HSVColorPickerView, @ColorInt color: Int)
    }

    init {

        context.obtainStyledAttributes(attrs, R.styleable.HSVColorPickerView).apply {
            mPointerDrawable = getDrawable(R.styleable.HSVColorPickerView_hcpv_pointerRes)
            mRadius = getDimension(R.styleable.HSVColorPickerView_hcpv_radius, 0f)
            mBrightnessMode = getBoolean(R.styleable.HSVColorPickerView_hcpv_brightnessMode, false)
            recycle()
        }

        isClickable = mEnable
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaintBackground = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaintBackground?.color = Color.WHITE
        mPaintMask = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaintMask?.color = Color.parseColor("#57000000")
        setLayerType(LAYER_TYPE_SOFTWARE, if (isInEditMode) null else mPaint)

        if (mPointerDrawable == null) {
            setPointerDrawable(R.mipmap.ic_color_picker_pointer)
        }
    }

    companion object {
        private val GRAD_COLORS = intArrayOf(
            Color.RED, Color.YELLOW, Color.GREEN,
            Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        )
        private val GRAD_ALPHA = intArrayOf(Color.WHITE, Color.TRANSPARENT)
    }
}