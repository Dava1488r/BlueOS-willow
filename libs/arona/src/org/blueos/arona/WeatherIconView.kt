package org.blueos.arona

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

/**
 * A weather icon (Material Symbols) that can live: the sun turns slowly, clouds drift, rain and snow fall,
 * lightning flashes. Tinted with [color] (the host passes its Monet text colour); the rain drops use the accent.
 */
class WeatherIconView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs), Choreographer.FrameCallback {

    var weather: Weather? = null
        set(value) { field = value; drawableFor(value); updateRunning(); invalidate() }
    var live = true
        set(value) { field = value; updateRunning(); invalidate() }
    var color = 0xffeeeeee.toInt()
        set(value) { field = value; icon?.setTint(value); invalidate() }
    /** Rain drops: the Monet accent (follows any wallpaper). */
    var dropColor = context.getColor(android.R.color.system_accent1_300)

    private var icon: android.graphics.drawable.Drawable? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private var t = 0f
    private var last = 0L
    private var running = false

    private fun drawableFor(w: Weather?) {
        icon = w?.let { context.getDrawable(it.icon)?.mutate()?.apply { setTint(color) } }
    }

    override fun onDraw(canvas: Canvas) {
        val d = icon ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        val kind = weather?.kind
        val moving = live && running
        drawnShift = if (moving) shift() else Float.NaN
        canvas.save()
        when {
            moving && kind == Weather.Kind.CLEAR && weather?.day == true -> canvas.rotate(t * 15f, w / 2, h / 2)
            moving && kind != Weather.Kind.CLEAR -> canvas.translate(sin(t * 2f * Math.PI.toFloat() / 6f) * w * 0.03f, 0f)
        }
        d.setBounds(0, 0, width, height)
        // lightning: a flash in the palette's tertiary accent
        if (moving && kind == Weather.Kind.STORM && (t % 4.5f) in 3.95f..4.1f) d.setTint(context.getColor(android.R.color.system_accent3_100)) else d.setTint(color)
        // 0.8: while they fall, only the cloud of the icon is drawn — its own big flakes / drops stood still under the
        // falling ones and looked like a mess. The cloud ends at 600/960 (snow), 640/960 (rain), the flakes / drops
        // start at 660 / 676 (Material Symbols); storm keeps its bolt
        val cloudOnly = moving && (kind == Weather.Kind.SNOW || kind == Weather.Kind.RAIN)
        if (cloudOnly) canvas.clipRect(0f, 0f, w, h * if (kind == Weather.Kind.SNOW) 0.66f else 0.69f)
        d.draw(canvas)
        canvas.restore()
        if (!moving) return
        if (kind == Weather.Kind.RAIN || kind == Weather.Kind.STORM) {
            paint.color = dropColor
            paint.strokeWidth = w * 0.05f
            for (i in 0..2) {
                val p = ((t + i / 3f) % 1f)
                paint.alpha = (sin(p * Math.PI.toFloat()) * 230).toInt()
                val x = w * (0.3f + i * 0.2f)
                val y = h * 0.72f + p * h * 0.18f
                canvas.drawLine(x, y, x - w * 0.03f, y + h * 0.1f, paint)
            }
        } else if (kind == Weather.Kind.SNOW) {
            // the icon's five flakes (two rows), each falling from under the cloud and melting away, one after another
            paint.color = color
            for (i in 0..4) {
                val p = (((t / 2.4f) + i / 5f) % 1f)
                paint.alpha = (sin(p * Math.PI.toFloat()) * 255).toInt()
                val x = w * (0.27f + (i % 3) * 0.25f + if (i >= 3) 0.125f else 0f) + sin(p * 6f + i) * w * 0.03f
                canvas.drawCircle(x, h * 0.70f + p * h * 0.26f, w * 0.05f, paint)
            }
        }
    }

    /** How far the icon itself has moved (px): the drift of a cloud, the turn of the sun at its rim. */
    private fun shift(): Float {
        val kind = weather?.kind
        return when {
            kind == Weather.Kind.CLEAR && weather?.day == true -> t * 15f * (Math.PI.toFloat() / 180f) * width / 2f
            kind != Weather.Kind.CLEAR -> sin(t * 2f * Math.PI.toFloat() / 6f) * width * 0.03f
            else -> 0f
        }
    }
    private var drawnShift = Float.NaN

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        // 0.8: off the screen it stands still; on it, a new frame only when something moved by a visible amount —
        // a cloud drifts ±2 px in 6 s and was redrawn 60 times a second (the night's moon doesn't move at all)
        if (!BlueOSOnScreen.visible(this)) {
            last = 0L
            Choreographer.getInstance().postFrameCallback(this)
            return
        }
        t += if (last == 0L) 0f else ((frameTimeNanos - last) / 1e9f).coerceAtMost(0.05f)
        last = frameTimeNanos
        val kind = weather?.kind
        val falling = kind == Weather.Kind.RAIN || kind == Weather.Kind.SNOW || kind == Weather.Kind.STORM
        if (falling || drawnShift.isNaN() || abs(shift() - drawnShift) >= BlueOSOnScreen.MIN_PX) invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun updateRunning(detaching: Boolean = false) {
        val want = !detaching && live && weather != null && isAttachedToWindow && isShown
        if (want == running) return
        running = want
        if (want) { last = 0L; Choreographer.getInstance().postFrameCallback(this) }
        else Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateRunning() }
    override fun onDetachedFromWindow() { updateRunning(detaching = true); super.onDetachedFromWindow() }
    override fun onVisibilityAggregated(isVisible: Boolean) { super.onVisibilityAggregated(isVisible); updateRunning() }
}
