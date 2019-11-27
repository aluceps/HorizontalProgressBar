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
    private var colorDecoration = DEFAULT_COLOR_DECORATION
    private var sizeBorder = DEFAULT_SIZE_BORDER
    private var sizeRadius = DEFAULT_SIZE_RADIUS
    private var sizeDecorationWidth = DEFAULT_SIZE_DECORATION_WIDTH
    private var imageForegroundResId = 0

    // 枠や角丸の属性情報は直接使わない
    private val border by lazy { if (decorationType == DecorationType.Tick) sizeBorder else 0f }
    private val radius by lazy { sizeRadius }

    // 目盛りを表示するときに使う枠内サイズ
    private val innerLeft by lazy { 0 + border }
    private val innerTop by lazy { 0 + border }
    private val innerRight by lazy { width - border }
    private val innerBottom by lazy { height - border }
    private val innerWidthWithoutTick by lazy { width - border * (2 + DEFAULT_TICK_COUNT - 1) }

    // 目盛りの間隔
    private val tickInterval by lazy { innerWidthWithoutTick / DEFAULT_TICK_COUNT }

    private val paintBackground by lazy {
        Paint().apply {
            color = colorBackground
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    private val paintInsideOfBackground by lazy {
        Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
            xfermode = when (decorationType) {
                // 内側をくり抜く
                DecorationType.Tick -> PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                // 通常の描画
                DecorationType.Line -> PorterDuffXfermode(PorterDuff.Mode.DST)
            }
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

    private val paintTick by lazy {
        Paint().apply {
            color = colorBackground
            style = Paint.Style.FILL
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
    }

    private val rectBackground by lazy { RectF(0f, 0f, width.toFloat(), height.toFloat()) }
    private val rectInsideOfBackground by lazy { RectF(innerLeft, innerTop, innerRight, innerBottom) }
    private val rectMask by lazy { rectInsideOfBackground.let { Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) } }

    private var progress = 0f
    private var bitmapForeground: Bitmap? = null

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
                    getColor(R.styleable.HorizontalProgressBar_progress_color_background, DEFAULT_COLOR_BACKGROUND).let { colorBackground = it }
                    getColor(R.styleable.HorizontalProgressBar_progress_color_foreground, DEFAULT_COLOR_FOREGROUND).let { colorForeground = it }
                }
            }
            getColor(R.styleable.HorizontalProgressBar_progress_color_decoration, DEFAULT_COLOR_DECORATION).let { colorDecoration = it }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_border, DEFAULT_SIZE_BORDER).let { sizeBorder = it }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_radius, DEFAULT_SIZE_RADIUS).let { sizeRadius = it }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_decoration_width, DEFAULT_SIZE_DECORATION_WIDTH).let { sizeDecorationWidth = it }
            getResourceId(R.styleable.HorizontalProgressBar_progress_image_foreground, 0).let { imageForegroundResId = it }
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

        // 背景部分の描画
        canvas.saveLayer(rectBackground, paintBackground)
        canvas.drawRoundRect(rectBackground, radius, radius, paintBackground)

        // プログレスの描画領域を調整
        when (decorationType) {
            DecorationType.Tick -> paintInsideOfBackground
            DecorationType.Line -> paintBackground
        }.let {
            canvas.drawRoundRect(rectInsideOfBackground, radius, radius, it)
        }

        // プログレスの描画
        val currentProgress = border + progress

        // 目盛りはプログレスに重ねて XOR するので
        // プログレスの描画よりも上にして描画する
        if (decorationType == DecorationType.Tick) {
            RectF().apply {
                set(innerLeft, innerTop, currentProgress, innerBottom)
            }.let {
                canvas.drawRect(it, paintProgress)
            }

            for (i in 1 until DEFAULT_TICK_COUNT) {
                val position = (tickInterval + border) * i
                RectF().apply {
                    set(position, innerTop, border + position, innerBottom)
                }.let {
                    canvas.drawRect(it, paintTick)
                }
            }
        }

        // 画像でプログレスを描画
        if (decorationType == DecorationType.Line) {
            canvas.drawRoundRect(rectInsideOfBackground, innerLeft, innerTop, Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST)
            })

            bitmapForeground = BitmapFactory.decodeResource(resources, imageForegroundResId)
            if (bitmapForeground == null) {
                canvas.drawRect(rectMask, paintProgress)
            } else {
                canvas.drawBitmap(bitmapForeground!!, null, rectInsideOfBackground, Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                })
            }

            RectF().apply {
                set(currentProgress, innerTop, innerRight, innerBottom)
            }.let {
                canvas.drawRect(it, Paint().apply {
                    color = colorBackground
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                })
            }
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

    fun setProgress(progress: Float) {
        val current = progress * innerWidthWithoutTick
        this.progress = when (decorationType) {
            DecorationType.Tick -> ((current / tickInterval).roundToInt() - 1).let { tickCount ->
                current + border * if (tickCount < 0) 0 else tickCount
            }
            DecorationType.Line -> current
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
        progress = 0f
    }

    companion object {
        private const val DEFAULT_COLOR_BACKGROUND = Color.LTGRAY
        private const val DEFAULT_COLOR_FOREGROUND = Color.GREEN
        private const val DEFAULT_COLOR_DECORATION = Color.GRAY
        private const val DEFAULT_SIZE_BORDER = 0f
        private const val DEFAULT_SIZE_RADIUS = 0f
        private const val DEFAULT_SIZE_DECORATION_WIDTH = 0f
        private const val DEFAULT_TICK_COUNT = 10
    }
}