package me.aluceps.horizontalprogressbar

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.*
import kotlin.math.roundToInt

class HorizontalProgressBar @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var withoutTick = DEFAULT_WITHOUT_TICK
    private var colorBackground = DEFAULT_COLOR_BACKGROUND
    private var colorForeground = DEFAULT_COLOR_FOREGROUND
    private var sizeBorder = DEFAULT_SIZE_BORDER
    private var sizeRadius = DEFAULT_SIZE_RADIUS

    // 目盛りを表示するときに使う枠内サイズ
    private val border by lazy { if (withoutTick) 0f else sizeBorder }
    private val radius by lazy { sizeRadius }
    private val innerLeft by lazy { 0 + border }
    private val innerTop by lazy { 0 + border }
    private val innerRight by lazy { width - border }
    private val innerBottom by lazy { height - border }
    private val innerWidthWithoutTick by lazy { width - border * (2 + DEFAULT_TICK_COUNT - 1) }
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
            xfermode = when (withoutTick) {
                true -> PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
                else -> PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }
        }
    }

    private val paintProgress by lazy {
        Paint().apply {
            color = colorForeground
            isAntiAlias = true
            xfermode = when (withoutTick) {
                true -> PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                else -> PorterDuffXfermode(PorterDuff.Mode.ADD)
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
    private val rectForeground = RectF()
    private val rectTick = RectF()

    private var progress = 0f

    init {
        setup(context, attrs, defStyleAttr)
    }

    @SuppressLint("Recycle")
    private fun setup(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        context?.obtainStyledAttributes(attrs, R.styleable.HorizontalProgressBar, defStyleAttr, 0)?.apply {
            getBoolean(R.styleable.HorizontalProgressBar_progress_without_tick, DEFAULT_WITHOUT_TICK).let { withoutTick = it }
            getColor(R.styleable.HorizontalProgressBar_progress_color_background, DEFAULT_COLOR_BACKGROUND).let { colorBackground = it }
            getColor(R.styleable.HorizontalProgressBar_progress_color_foreground, DEFAULT_COLOR_FOREGROUND).let { colorForeground = it }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_border, DEFAULT_SIZE_BORDER).let { sizeBorder = it }
            getDimension(R.styleable.HorizontalProgressBar_progress_size_radius, DEFAULT_SIZE_RADIUS).let { sizeRadius = it }
        }?.recycle()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                post { invalidate() }
            }
        }, 10, 10)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        canvas.saveLayer(rectBackground, paintBackground)
        canvas.drawRoundRect(rectBackground, radius, radius, paintBackground)

        when (withoutTick) {
            true -> paintBackground
            else -> paintInsideOfBackground
        }.let {
            canvas.drawRoundRect(rectInsideOfBackground, radius, radius, it)
        }

        rectForeground.apply {
            set(innerLeft, innerTop, border + progress, innerBottom)
        }.let {
            canvas.drawRect(it, paintProgress)
        }

        if (!withoutTick) {
            for (i in 1 until DEFAULT_TICK_COUNT) {
                val position = (tickInterval + border) * i
                rectTick.apply {
                    set(position, innerTop, border + position, innerBottom)
                }.let {
                    canvas.drawRect(it, paintTick)
                }
            }
        }

        canvas.restore()
    }

    fun setProgress(progress: Float) {
        val current = progress * innerWidthWithoutTick
        this.progress = when (withoutTick) {
            true -> current
            else -> ((current / tickInterval).roundToInt() - 1).let { tickCount ->
                current + border * if (tickCount < 0) 0 else tickCount
            }
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
        private const val DEFAULT_WITHOUT_TICK = false
        private const val DEFAULT_COLOR_BACKGROUND = Color.LTGRAY
        private const val DEFAULT_COLOR_FOREGROUND = Color.GREEN
        private const val DEFAULT_SIZE_BORDER = 0f
        private const val DEFAULT_SIZE_RADIUS = 0f
        private const val DEFAULT_TICK_COUNT = 10
    }
}