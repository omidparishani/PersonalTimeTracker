package com.personal.timetracker.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils

data class BarItem(val label: String, val value: Int, val color: Int)
data class DonutItem(val label: String, val value: Int, val color: Int)

/**
 * Animated horizontal bar chart, drawn right-to-left to match RTL layout.
 * No external chart library required.
 */
class BarChartView(context: Context) : View(context) {

    var items: List<BarItem> = emptyList()
        set(value) {
            field = value
            requestLayout()
            startAnim()
        }
    var maxValue: Int = 0 // 0 = auto (max of items)
    var trackColor: Int = Color.LTGRAY
    var labelColor: Int = Color.DKGRAY

    private val density = context.resources.displayMetrics.density
    private val labelHeightPx = 18 * density
    private val barHeightPx = 20 * density
    private val gapPx = 6 * density
    private val rowSpacingPx = 16 * density // breathing room between one row's bar and the next label
    private val rowHeightPx = labelHeightPx + gapPx + barHeightPx + rowSpacingPx

    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12.5f * density
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun startAnim() {
        animator?.cancel()
        animProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rows = items.size.coerceAtLeast(1)
        val h = (paddingTop + paddingBottom + rows * rowHeightPx).toInt()
        setMeasuredDimension(w, if (items.isEmpty()) 0 else h)
    }

    override fun onDraw(canvas: Canvas) {
        if (items.isEmpty()) return
        val w = width.toFloat()
        val maxV = (maxValue.takeIf { it > 0 } ?: items.maxOf { it.value }).coerceAtLeast(1)
        labelPaint.color = labelColor
        val trackLeft = paddingLeft.toFloat()
        val trackRightEdge = w - paddingRight
        val centerX = (trackLeft + trackRightEdge) / 2f

        items.forEachIndexed { i, item ->
            val top = paddingTop + i * rowHeightPx
            canvas.drawText(
                item.label,
                centerX,
                top + labelHeightPx * 0.72f,
                labelPaint
            )
            val trackTop = top + labelHeightPx + gapPx
            val trackBottom = trackTop + barHeightPx
            val trackRect = RectF(trackLeft, trackTop, trackRightEdge, trackBottom)
            trackPaint.color = trackColor
            canvas.drawRoundRect(trackRect, barHeightPx / 2f, barHeightPx / 2f, trackPaint)

            val frac = (item.value.toFloat() / maxV).coerceIn(0f, 1f) * animProgress
            val barWidth = trackRect.width() * frac
            if (barWidth > 1.5f) {
                val barRect = RectF(trackRect.right - barWidth, trackTop, trackRect.right, trackBottom)
                val lighter = ColorUtils.blendARGB(item.color, Color.WHITE, 0.25f)
                barPaint.shader = LinearGradient(
                    barRect.left, 0f, barRect.right, 0f,
                    lighter, item.color, Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(barRect, barHeightPx / 2f, barHeightPx / 2f, barPaint)
                barPaint.shader = null
            }
        }
    }
}

/**
 * Animated donut chart with a centered title/subtitle. No external library required.
 */
class DonutChartView(context: Context) : View(context) {

    var items: List<DonutItem> = emptyList()
        set(value) {
            field = value
            startAnim()
        }
    var trackColor: Int = Color.LTGRAY
    var centerTitle: String = ""
    var centerSubtitle: String = ""
    var titleColor: Int = Color.BLACK
    var subtitleColor: Int = Color.GRAY

    private val density = context.resources.displayMetrics.density
    private var animProgress = 0f
    private var animator: ValueAnimator? = null
    private val rect = RectF()

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private fun startAnim() {
        animator?.cancel()
        animProgress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 750
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desired = if (w > 0) w else (160 * density).toInt()
        setMeasuredDimension(desired, desired)
    }

    override fun onDraw(canvas: Canvas) {
        val strokeW = width * 0.16f
        arcPaint.strokeWidth = strokeW
        val pad = strokeW / 2f + 2f * density
        rect.set(pad, pad, width - pad, height - pad)

        arcPaint.color = trackColor
        canvas.drawArc(rect, 0f, 360f, false, arcPaint)

        val total = items.sumOf { it.value }.coerceAtLeast(0)
        if (total > 0) {
            var start = -90f
            items.forEach { item ->
                val sweepFull = (item.value.toFloat() / total) * 360f
                arcPaint.color = item.color
                canvas.drawArc(rect, start, sweepFull * animProgress, false, arcPaint)
                start += sweepFull
            }
        }

        val cx = width / 2f
        val cy = height / 2f
        titlePaint.color = titleColor
        titlePaint.textSize = width * 0.12f
        subtitlePaint.color = subtitleColor
        subtitlePaint.textSize = width * 0.062f

        if (centerTitle.isNotEmpty() && centerSubtitle.isNotEmpty()) {
            val gap = 4f * density
            val titleFm = titlePaint.fontMetrics
            val subFm = subtitlePaint.fontMetrics
            val titleH = titleFm.descent - titleFm.ascent
            val subH = subFm.descent - subFm.ascent
            val blockH = titleH + gap + subH
            val blockTop = cy - blockH / 2f
            val titleBaseline = blockTop - titleFm.ascent
            val subBaseline = blockTop + titleH + gap - subFm.ascent
            canvas.drawText(centerTitle, cx, titleBaseline, titlePaint)
            canvas.drawText(centerSubtitle, cx, subBaseline, subtitlePaint)
        } else if (centerTitle.isNotEmpty()) {
            val fm = titlePaint.fontMetrics
            canvas.drawText(centerTitle, cx, cy - (fm.ascent + fm.descent) / 2f, titlePaint)
        }
    }
}

object ChartHelper {
    /** Builds a legend row (colored dot + label + value) list under a donut chart. */
    fun legend(
        ctx: Context,
        entries: List<Triple<String, String, Int>>, // label, valueText, color
        dark: Boolean
    ): LinearLayout {
        val density = ctx.resources.displayMetrics.density
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        entries.forEach { (label, valueText, color) ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }
            val dot = View(ctx).apply {
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(color)
                }
                background = gd
                layoutParams = LinearLayout.LayoutParams((10 * density).toInt(), (10 * density).toInt()).apply {
                    marginStart = (2 * density).toInt()
                    marginEnd = (10 * density).toInt()
                }
            }
            val labelTv = TextView(ctx).apply {
                text = label
                textSize = 12.5f
                setTextColor(ThemeHelper.textPrimary(dark))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valueTv = TextView(ctx).apply {
                text = valueText
                textSize = 12.5f
                setTextColor(ThemeHelper.textSecondary(dark))
            }
            row.addView(dot)
            row.addView(labelTv)
            row.addView(valueTv)
            box.addView(row)
        }
        return box
    }
}
