package me.aluceps.horizontalprogressbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

    private val progressPrimary by lazy {
        Paint().apply {
            this.strokeWidth = strokeWidth
            this.color = colorPrimary
            this.isAntiAlias = true
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }
    }

    private val rectBase by lazy { RectF(0f, 0f, width.toFloat(), height.toFloat()) }
    private val rectPrimary by lazy { RectF(innerLeft, innerTop, innerRight, innerBottom) }

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
        canvas.drawRoundRect(rectPrimary, innerRadius, innerRadius, progressPrimary)
    }
}