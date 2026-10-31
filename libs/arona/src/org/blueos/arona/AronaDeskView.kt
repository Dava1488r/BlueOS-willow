package org.blueos.arona

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.view.Choreographer
import android.view.View
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.android.AndroidSkeletonDrawable
import com.esotericsoftware.spine.android.SkeletonRenderer
import kotlin.math.min

/**
 * 0.7.2: Arona at her desk, from the game's work page (AronaModel.newDesk): sitting on it and swinging her legs
 * (Idle_02) or asleep at it (Idle_00, at night). The desk is drawn in the palette's colours — the game's wood
 * turned grey and tinted with a Monet tone ([deskTint]) — so it doesn't stand out on the widget's card; the
 * draw order is kept (sitting: the desk is behind her; asleep: she is behind it, her arms on it).
 */
class AronaDeskView(context: Context) : View(context), Choreographer.FrameCallback {
    var asleep = false
        set(value) { if (field != value) { field = value; pose() } }

    /** The desk's colour (a Monet tone, set by the widget for its card); its wood keeps the light and shade. */
    var deskTint = Color.LTGRAY
        set(value) { field = value; deskPaint.colorFilter = filter(value) }

    var onReady: (() -> Unit)? = null
    val isReady get() = drawable != null

    private var drawable: AndroidSkeletonDrawable? = null
    private val renderer = SkeletonRenderer()
    private val deskPaint = Paint().apply { colorFilter = filter(deskTint) }
    private var running = false
    private var lastFrame = 0L
    private var alphas = FloatArray(0)

    init {
        AronaModel.newDesk { d ->
            drawable = d ?: return@newDesk
            pose()
            updateRunning()
            onReady?.invoke()
        }
    }

    private fun pose() {
        val d = drawable ?: return
        d.animationState.setAnimation(0, if (asleep) "Idle_00" else "Idle_02", true)
        invalidate()
    }

    /** The world rectangle of the pose (measured in spine-models/desk.html over the whole animation). */
    val viewport get() = if (asleep) SLEEP else SIT

    /** Her head in this view's pixels, for the reply chip beside it. */
    fun headRect(out: RectF): RectF {
        val sk = drawable?.skeleton
        val b = sk?.findBone(if (asleep) "A_00_Head" else "A_02_Head_01")
        val s = scale()
        val wx = b?.worldX ?: (viewport.x + viewport.w / 2)
        val wy = b?.worldY ?: (viewport.y + viewport.h * 0.8f)
        val cx = offsetX(s) + (wx - viewport.x) * s
        val cy = height - (wy - viewport.y) * s
        val r = 200f * s
        return out.apply { set(cx - r, cy - r, cx + r, cy + r) }
    }

    private fun filter(tint: Int): ColorMatrixColorFilter {
        // grey (luminance) × the tint, a touch lighter so the wood's light parts reach the tint itself
        val r = Color.red(tint) / 255f * 1.15f
        val g = Color.green(tint) / 255f * 1.15f
        val b = Color.blue(tint) / 255f * 1.15f
        return ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0.3f * r, 0.59f * r, 0.11f * r, 0f, 0f,
            0.3f * g, 0.59f * g, 0.11f * g, 0f, 0f,
            0.3f * b, 0.59f * b, 0.11f * b, 0f, 0f,
            0f, 0f, 0f, 0.92f, 0f)))
    }

    private fun isDesk(s: Slot) = s.data.name.startsWith("A_00_Desk")

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        // 0.8: like her: off the screen the desk scene holds still
        if (!BlueOSOnScreen.visible(this)) {
            lastFrame = 0L
            Choreographer.getInstance().postFrameCallback(this)
            return
        }
        val dt = if (lastFrame == 0L) 0f else ((frameTimeNanos - lastFrame) / 1e9f).coerceIn(0f, 0.05f)
        lastFrame = frameTimeNanos
        drawable?.let {
            it.animationState.update(dt)
            it.animationState.apply(it.skeleton)
            it.skeleton.update(dt)
            it.skeleton.updateWorldTransform(Skeleton.Physics.update)
        }
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun updateRunning(detaching: Boolean = false) {
        val want = !detaching && isAttachedToWindow && isShown && drawable != null
        if (want == running) return
        running = want
        if (want) { lastFrame = 0L; Choreographer.getInstance().postFrameCallback(this) }
        else Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateRunning() }
    override fun onDetachedFromWindow() { updateRunning(detaching = true); super.onDetachedFromWindow() }
    override fun onVisibilityAggregated(isVisible: Boolean) { super.onVisibilityAggregated(isVisible); updateRunning() }

    private fun scale() = if (width == 0 || height == 0) 1f else min(width / viewport.w, height / viewport.h)
    private fun offsetX(s: Float) = (width - viewport.w * s) / 2f

    override fun onDraw(canvas: Canvas) {
        val sk = drawable?.skeleton ?: return
        val order = sk.drawOrder
        if (alphas.size != order.size) alphas = FloatArray(order.size)
        for (i in 0 until order.size) alphas[i] = order[i].color.a
        val first = (0 until order.size).firstOrNull { isDesk(order[it]) } ?: order.size
        val last = (0 until order.size).lastOrNull { isDesk(order[it]) } ?: -1
        val s = scale()
        canvas.save()
        canvas.translate(offsetX(s), height.toFloat())
        canvas.scale(s, -s)
        canvas.translate(-viewport.x, -viewport.y)
        // what's behind the desk, the desk (recoloured), what's in front of it
        pass(canvas, sk) { it < first }
        if (last >= 0) {
            canvas.saveLayer(null, deskPaint)
            pass(canvas, sk) { it in first..last && isDesk(order[it]) }
            canvas.restore()
        }
        pass(canvas, sk) { it > last || (it in first..last && !isDesk(order[it])) }
        canvas.restore()
        for (i in 0 until order.size) order[i].color.a = alphas[i]
    }

    private inline fun pass(canvas: Canvas, sk: Skeleton, keep: (Int) -> Boolean) {
        val order = sk.drawOrder
        for (i in 0 until order.size) order[i].color.a = if (keep(i)) alphas[i] else 0f
        renderer.renderToCanvas(canvas, renderer.render(sk))
    }

    companion object {
        val SIT = AronaView.Viewport(-1358f, 73f, 1038f, 1529f)
        val SLEEP = AronaView.Viewport(-1585f, 73f, 1063f, 1105f)
    }
}
