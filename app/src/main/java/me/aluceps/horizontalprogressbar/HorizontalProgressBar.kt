package me.aluceps.horizontalprogressbar

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
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

    private var borderWidth = 0f
    private var cornerRadius = 0f
    private var colorBase = 0
    private var colorValue = 0

    private val innerLeft by lazy { 0 + borderWidth }
    private val innerTop by lazy { 0 + borderWidth }
    private val innerRight by lazy { width - borderWidth }
    private val innerBottom by lazy { height - borderWidth }
    private val innerRadius by lazy { cornerRadius - borderWidth }

    private val innerWidthWithoutTick by lazy {
        width - borderWidth * (2 + TICK_COUNT - 1)
    }

    private val tickInterval by lazy {
        innerWidthWithoutTick / TICK_COUNT
    }

    private val progressBase by lazy {
        Paint().apply {
            color = colorBase
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    private val progressInner by lazy {
        Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
    }

    private val progressValue by lazy {
        Paint().apply {
            color = colorValue
            isAntiAlias = true
//            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
    }

    private val tickBase by lazy {
        Paint().apply {
            color = colorBase
            style = Paint.Style.FILL
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
    }

    private val rectBase by lazy { RectF(0f, 0f, width.toFloat(), height.toFloat()) }
    private val rectInner by lazy { RectF(innerLeft, innerTop, innerRight, innerBottom) }
    private val rectValue = RectF()
    private val rectTick = RectF()

    private var progress = 0f

    init {
        setup(context, attrs, defStyleAttr)
    }

    private fun setup(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context?.obtainStyledAttributes(attrs, R.styleable.HorizontalProgressBar, defStyleAttr, 0)
        typedArray?.let { t ->
            t.getDimension(R.styleable.HorizontalProgressBar_progress_border_width, 0f)
                .let { borderWidth = it }
            t.getDimension(R.styleable.HorizontalProgressBar_progress_corner_radius, 0f)
                .let { cornerRadius = it }
            t.getColor(R.styleable.HorizontalProgressBar_progress_color_base, Color.WHITE)
                .let { colorBase = it }
            t.getColor(R.styleable.HorizontalProgressBar_progress_color_value, Color.LTGRAY)
                .let { colorValue = it }
        }
        typedArray?.recycle()

        Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    post { invalidate() }
                }
            }, 10, 10)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        canvas.saveLayer(rectBase, progressBase)
        canvas.drawRoundRect(rectBase, cornerRadius, cornerRadius, progressBase)
        canvas.drawRoundRect(rectInner, innerRadius, innerRadius, progressInner)

        rectValue.also {
            it.set(innerLeft, innerTop, borderWidth + progress, innerBottom)
        }.let {
            canvas.drawRect(it, progressValue)
        }

        for (i in 1 until TICK_COUNT) {
            val position = (tickInterval + borderWidth) * i
            rectTick.also {
                it.set(position, innerTop, borderWidth + position, innerBottom)
            }.let {
                canvas.drawRect(it, tickBase)
            }
        }

        canvas.restore()
    }

    fun setProgress(progress: Float) {
        val current = progress * innerWidthWithoutTick
        val tickCount = (current / tickInterval).roundToInt() - 1
        this.progress = current + borderWidth * if (tickCount < 0) 0 else tickCount
    }

    fun reset() {
        progress = 0f
    }

    fun blink() {
        ValueAnimator().apply {
            setIntValues(Color.TRANSPARENT, colorValue)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { progressValue.color = it.animatedValue as Int }
            duration = 300
            interpolator = LinearInterpolator()
            repeatCount = 1
        }.start()
    }

    companion object {
        private const val TICK_COUNT = 10
    }
}