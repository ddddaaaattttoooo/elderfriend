package ge.elderfriend.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Four rounded signal bars instead of the old block glyphs. */
class SignalBars @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private var level = 3
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val active = Color.WHITE
    private val inactive = Color.parseColor("#4DFFFFFF")

    fun setLevel(value: Int) {
        level = value.coerceIn(0, 4)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val gap = w * 0.09f
        val barW = (w - gap * 3f) / 4f
        for (i in 0 until 4) {
            val fraction = 0.40f + i * 0.20f
            val left = i * (barW + gap)
            val top = h - h * fraction
            rect.set(left, top, left + barW, h)
            paint.color = if (i < level) active else inactive
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }
}
