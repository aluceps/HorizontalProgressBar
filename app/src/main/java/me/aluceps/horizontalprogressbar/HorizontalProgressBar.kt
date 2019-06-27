package me.aluceps.horizontalprogressbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*

class HorizontalProgressBar @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var borderWidth = 0f
    private var cornerRadius = 0f
    private var colorBase = 0
    private var colorPrimary = 0
    private var colorSecondary = 0

    /**
     * 枠を表現するために枠の太さを加減
     */
    private val innerLeft by lazy { 0 + borderWidth }
    private val innerTop by lazy { 0 + borderWidth }
    private val innerRight by lazy { width - borderWidth }
    private val innerBottom by lazy { height - borderWidth }
    private val innerRadius by lazy { cornerRadius - borderWidth }

    private val progressBase by lazy {
        Paint().apply {
            this.strokeWidth = strokeWidth
            this.color = colorBase
            this.isAntiAlias = true
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }
    }

    private val progressCutting by lazy {
        Paint().apply {
            this.strokeWidth = strokeWidth
            this.isAntiAlias = true
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
            this.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private val progressPrimary by lazy {
        Paint().apply {
            this.strokeWidth = strokeWidth
            this.color = colorPrimary
            this.isAntiAlias = true
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
            this.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
        }
    }

    private val rectBase by lazy { RectF(0f, 0f, width.toFloat(), height.toFloat()) }
    private val rectCutting by lazy { RectF(innerLeft, innerTop, innerRight, innerBottom) }
    private val rectPrimary = RectF()

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
            t.getColor(R.styleable.HorizontalProgressBar_progress_color_base, Color.GRAY)
                .let { colorBase = it }
            t.getColor(R.styleable.HorizontalProgressBar_progress_color_primary, Color.WHITE)
                .let { colorPrimary = it }
            t.getColor(R.styleable.HorizontalProgressBar_progress_color_secondary, Color.LTGRAY)
                .let { colorSecondary = it }
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
        canvas.drawRoundRect(rectBase, cornerRadius, cornerRadius, progressBase)
        canvas.drawRoundRect(rectCutting, innerRadius, innerRadius, progressCutting)
        rectPrimary.also { it.set(innerLeft, innerTop, innerRight * progress, innerBottom) }.let {
            canvas.drawRect(it, progressPrimary)
        }
    }

    fun setProgress(progress: Float) {
        this.progress = progress
    }
}