package me.aluceps.horizontalprogressbar

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.*
import kotlin.math.roundToInt

enum class DecorationType(val id: Int) {
    Tick(0),
    Line(1);

    companion object {
        fun fromInt(value: Int) =
                values().find { it.id == value } ?: Tick
    }
}

class HorizontalProgressBar @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var decorationType = DecorationType.Tick
    private var colorBackground = DEFAULT_COLOR_BACKGROUND
    private var colorForeground = DEFAULT_COLOR_FOREGROUND
    private var sizeBorder = DEFAULT_SIZE_BORDER
    private var sizeRadius = DEFAULT_SIZE_RADIUS
    private var imageForegroundResId = 0

    // 枠や角丸の属性情報は直接使わない
    private val border get() = if (decorationType == DecorationType.Tick) sizeBorder else 0f
    private val radius get() = sizeRadius

    // 目盛りを表示するときに使う枠内サイズ
    private val innerLeft get() = 0 + border
    private val innerTop get() = 0 + border
    private val innerRight get() = width - border
    private val innerBottom get() = height - border
    private val innerWidthWithoutTick get() = width - border * (2 + DEFAULT_TICK_COUNT - 1)

    // 目盛りの間隔
    private val tickInterval get() = innerWidthWithoutTick / DEFAULT_TICK_COUNT

    private val paintBackground by lazy {
        Paint().apply {
            color = colorBackground
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    private val paintProgress by lazy {
        Paint().apply {
            color = colorForeground
            isAntiAlias = true
            xfermode = when (decorationType) {
                // 透明な部分に着色
                DecorationType.Tick -> PorterDuffXfermode(PorterDuff.Mode.ADD)
                // 重なる部分に着色
                DecorationType.Line -> PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private val rectBackground by lazy { RectF(0f, 0f, width.toFloat(), height.toFloat()) }
    private val rectInsideOfBackground by lazy { RectF(innerLeft, innerTop, innerRight, innerBottom) }
    private var bitmapForeground: Bitmap? = null

    var progress = DEFAULT_PROGRESS_VALUE
    private val progressWidth: Float
        get() {
            val width = progress * innerWidthWithoutTick
            return when (decorationType) {
                DecorationType.Tick -> if (progress == DEFAULT_PROGRESS_VALUE) {
                    // プログレスの表示が後ろにはみでちゃう
                    border
                } else {
                    // 表示幅に応じて目盛りを考慮してあげる
                    (width / tickInterval).roundToInt().let { width + border * if (it < 0) 0 else it }
                }
                DecorationType.Line -> width
            }
        }

    init {
        setup(context, attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun setup(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        context?.obtainStyledAttributes(attrs, R.styleable.HorizontalProgressBar, defStyleAttr, 0)?.apply {
            getInt(R.styleable.HorizontalProgressBar_progress_decoration_type, DecorationType.Tick.id).let { decorationType = DecorationType.fromInt(it) }
            when (decorationType) {
                DecorationType.Tick -> {
                    colorBackground = Color.WHITE
                    colorForeground = Color.WHITE
                }
                DecorationType.Line -> {
                    getInteger(R.styleable.HorizontalProgressBar_progress_color_background, DEFAULT_COLOR_BACKGROUND).let { colorBackground = it }
                    getInteger(R.styleable.HorizontalProgressBar_progress_color_foreground, DEFAULT_COLOR_FOREGROUND).let { colorForeground = it }
                }
            }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_border, DEFAULT_SIZE_BORDER).let { sizeBorder = it }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_radius, DEFAULT_SIZE_RADIUS).let { sizeRadius = it }
            getResourceId(R.styleable.HorizontalProgressBar_progress_image_foreground, 0).let { imageForegroundResId = it }
            if (progress == DEFAULT_PROGRESS_VALUE) {
                getFloat(R.styleable.HorizontalProgressBar_progress_value, DEFAULT_PROGRESS_VALUE).let { progress = it }
            }
        }?.recycle()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                post { invalidate() }
            }
        }, 10, 10)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return
        canvas.saveLayer(rectBackground, paintBackground)

        // 背景部分の描画
        drawBackground(canvas)

        // プログレスの描画領域を調整
        drawInsideOfBackground(canvas)

        // プログレスの描画
        when (decorationType) {
            DecorationType.Tick -> drawProgressWithTick(canvas, progressWidth)
            DecorationType.Line -> drawProgressWithDrawable(canvas, progressWidth)
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (bitmapForeground != null && bitmapForeground?.isRecycled != true) {
            bitmapForeground?.recycle()
            bitmapForeground = null
        }
    }

    private fun drawBackground(canvas: Canvas) {
        when (decorationType) {
            DecorationType.Tick -> rectBackground to paintBackground
            DecorationType.Line -> rectInsideOfBackground to Paint().apply {
                isAntiAlias = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST)
            }
        }.let { (rect, paint) ->
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    private fun drawInsideOfBackground(canvas: Canvas) {
        when (decorationType) {
            DecorationType.Tick -> rectInsideOfBackground to Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            DecorationType.Line -> rectInsideOfBackground to paintBackground
        }.let { (rect, paint) ->
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    private fun drawProgressWithTick(canvas: Canvas, progress: Float) {
        RectF().apply {
            set(innerLeft, innerTop, progress, innerBottom)
        }.let {
            canvas.drawRect(it, paintProgress)
        }

        for (i in 1 until DEFAULT_TICK_COUNT) {
            val position = (tickInterval + border) * i
            RectF().apply {
                set(position, innerTop, border + position, innerBottom)
            }.let {
                canvas.drawRect(it, Paint().apply {
                    color = colorBackground
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
                })
            }
        }
    }

    private fun drawProgressWithDrawable(canvas: Canvas, progress: Float) {
        bitmapForeground = BitmapFactory.decodeResource(resources, imageForegroundResId)
        if (bitmapForeground == null) {
            canvas.drawRect(rectInsideOfBackground.toInt(), paintProgress)
        } else {
            canvas.drawBitmap(bitmapForeground!!, null, rectInsideOfBackground, Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            })
        }

        // プログレスは開始座標Xを動かす
        RectF().apply {
            set(progress, innerTop, innerRight, innerBottom)
        }.let {
            canvas.drawRect(it, Paint().apply {
                color = colorBackground
                isAntiAlias = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            })
        }

        // 縁取り
        if (sizeBorder > 0f) {
            canvas.drawRoundRect(rectInsideOfBackground.setMargin(sizeBorder * 0.5f), radius, radius, Paint().apply {
                color = Color.argb(10, 0, 0, 0)
                style = Paint.Style.STROKE
                strokeWidth = sizeBorder
                isAntiAlias = true
            })
        }
    }

    fun blink() {
        ValueAnimator().apply {
            setIntValues(Color.TRANSPARENT, colorForeground)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { paintProgress.color = it.animatedValue as Int }
            duration = 300
            interpolator = LinearInterpolator()
            repeatCount = 1
        }.start()
    }

    fun reset() {
        progress = DEFAULT_PROGRESS_VALUE
    }

    fun setDecorationType(type: DecorationType) {
        decorationType = type
    }

    private fun RectF.toInt() = Rect(
            left.toInt(),
            top.toInt(),
            right.toInt(),
            bottom.toInt()
    )

    private fun RectF.setMargin(margin: Float) = RectF(
            left + margin,
            top + margin,
            right - margin,
            bottom - margin
    )

    companion object {
        private const val DEFAULT_COLOR_BACKGROUND = Color.LTGRAY
        private const val DEFAULT_COLOR_FOREGROUND = Color.GREEN
        private const val DEFAULT_SIZE_BORDER = 0f
        private const val DEFAULT_SIZE_RADIUS = 0f
        private const val DEFAULT_PROGRESS_VALUE = 0f
        private const val DEFAULT_TICK_COUNT = 10
    }
}