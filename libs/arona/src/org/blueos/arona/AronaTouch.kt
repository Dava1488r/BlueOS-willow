package org.blueos.arona

import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The same "brain" for the lock screen and the widget: touch → look at the finger / pat on the head,
 * a short tap → a reply; pats wake her up at night (she falls asleep again after 6 s).
 * Touches that miss her body return false, so swipes elsewhere keep working.
 */
class AronaTouch(
    private val arona: AronaView,
    /** Where the reply chip lives (a full-size FrameLayout above everything that contains [chip]). */
    private val host: () -> View,
    private val chip: ReplyChip,
    /** Rects in [host] coordinates the chip must not cover (clock, halo…). */
    private val avoid: () -> List<RectF> = { emptyList() },
    private val isNight: () -> Boolean = { false },
    /** Called after every tap/pat (the lock screen's dynamic mode counts them). */
    private val touched: () -> Unit = {},
    /** 0.7.1: true = the system must not take this gesture away (patting her head); false = give it back. */
    private val grab: (Boolean) -> Unit = {},
    /** 0.7.1: the finger started moving on her (the launcher cancels its long press = no widget pick-up). */
    private val dragged: () -> Unit = {},
    /** 0.7.1: a rect the chip stays out of — above or below it (the widget's card). */
    private val outside: () -> RectF? = { null },
) {
    private val ctx = arona.context
    private val dp = ctx.resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var moved = 0f
    private var kind = AronaView.NONE
    private var active = false
    private var grabbed = false
    private var gazing = false

    fun onTouch(e: MotionEvent): Boolean {
        val cr = ctx.contentResolver
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!arona.isReady || !arona.isOnBody(e.x, e.y)) return false
                active = true
                gazing = false
                grabbed = false
                handler.removeCallbacks(gazeEnd)
                downX = e.x; downY = e.y; downTime = SystemClock.uptimeMillis(); moved = 0f
                val w = arona.toWorld(e.x, e.y)
                kind = when {
                    arona.isOnHead(w[0], w[1]) && BlueOSSettings.on(cr, BlueOSSettings.ARONA_PAT) -> AronaView.PAT
                    !arona.sleeping && BlueOSSettings.on(cr, BlueOSSettings.ARONA_LOOK) -> AronaView.LOOK
                    else -> AronaView.NONE
                }
                if (kind != AronaView.NONE) arona.begin(kind, w[0], w[1])
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!active) return false
                val dx = e.x - downX
                val dy = e.y - downY
                moved = maxOf(moved, hypot(dx, dy))
                if (kind == AronaView.PAT && !grabbed && moved > 3 * dp) {
                    // patting: claim the gesture before the shade / the home screen pager / the long press
                    // take it (their slop is ~8 dp); a still finger keeps the launcher's long press
                    grabbed = true
                    grab(true)
                    dragged()
                }
                if (grabbed && SystemClock.uptimeMillis() - downTime < 300 && dy < -48 * dp && abs(dy) > 2 * abs(dx)) {
                    // a quick swipe up that started on her head is still "unlock" / "all apps"
                    grabbed = false
                    grab(false)
                    arona.end()
                    kind = AronaView.NONE
                    active = false
                    return false
                }
                if (kind != AronaView.NONE) arona.toWorld(e.x, e.y).let { arona.moveTo(it[0], it[1]) }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!active) return false
                active = false
                if (grabbed) { grabbed = false; grab(false) }
                arona.end()
                if (e.actionMasked == MotionEvent.ACTION_CANCEL) return true
                val quick = SystemClock.uptimeMillis() - downTime < 300 && moved < 12 * dp
                if (kind == AronaView.PAT && !quick) {
                    if (arona.sleeping) {
                        say(AronaReplies.Kind.SLEEP_WAKE)
                        arona.setSleeping(false)
                        handler.postDelayed({ if (isNight()) arona.setSleeping(true) }, 6000)
                    } else say(AronaReplies.Kind.PAT)
                    touched()
                } else if (quick) {
                    say(if (arona.sleeping) AronaReplies.Kind.SLEEP else AronaReplies.Kind.TAP)
                    touched()
                }
                return true
            }
        }
        return active
    }

    /**
     * 0.7.1: a touch anywhere on the screen (raw coordinates, from [AronaGaze]; observed, never consumed):
     * she looks at the finger while it is down, then back.
     */
    fun gaze(action: Int, rawX: Float, rawY: Float) {
        if (active) return                                   // a touch on her: [onTouch] handles it
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val cr = ctx.contentResolver
                if (!arona.isReady || !arona.isShown || arona.sleeping || arona.touchMode != AronaView.NONE ||
                    !BlueOSSettings.on(cr, BlueOSSettings.ARONA_LOOK) || !BlueOSSettings.on(cr, BlueOSSettings.ARONA_GAZE)) return
                handler.removeCallbacks(gazeEnd)
                gazing = true
                worldOf(rawX, rawY).let { arona.begin(AronaView.LOOK, it[0], it[1]) }
            }
            MotionEvent.ACTION_MOVE -> if (gazing) worldOf(rawX, rawY).let { arona.moveTo(it[0], it[1]) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (gazing) {
                gazing = false
                handler.postDelayed(gazeEnd, 450)            // keeps looking there for a moment
            }
        }
    }

    private val gazeEnd = Runnable { if (!active && !gazing) arona.end() }

    private fun worldOf(rawX: Float, rawY: Float): FloatArray {
        val loc = IntArray(2).also { arona.getLocationOnScreen(it) }
        return arona.toWorld(rawX - loc[0], rawY - loc[1])
    }

    /**
     * Picks a line, plays it by the user's choice and shows the chip beside her head. [silent]: a chip only
     * (things she does by herself, like sitting down at her desk, never make a sound). [at]: the view whose
     * head the chip goes beside (her desk view in the widget, 0.7.2); her own face changes only on [arona].
     */
    fun say(kind: AronaReplies.Kind, silent: Boolean = false, at: View? = null) {
        // (0.7.1: no ARONA_SHOW check — that switch is the lock screen's; it used to silence the widget too)
        val h0 = host()
        if (chip.parent == null || h0.width == 0) return
        val c = arona.character
        val line = AronaReplies.pick(ctx, kind, c)
        // 0.7.1: she opens her mouth with her voice, and her face shows the line's emotion
        val voice = if (silent) null else AronaReplies.play(ctx, kind, line, c)
        voice?.let { arona.talk(AronaReplies.envelope(it.name), it.ms) }
        if (at == null && !(arona.sleeping && kind == AronaReplies.Kind.SLEEP)) {
            arona.face(AronaReplies.face(kind, line, c), maxOf((voice?.ms ?: 0) + 400L, 2600L))
        }
        val owner = at ?: arona
        val head = if (at is AronaDeskView) at.headRect(RectF()) else arona.headRect(RectF())
        val a = IntArray(2).also { owner.getLocationInWindow(it) }
        val h = IntArray(2).also { h0.getLocationInWindow(it) }
        head.offset((a[0] - h[0]).toFloat(), (a[1] - h[1]).toFloat())
        chip.show(line, kind, head, avoid(), h0.width, h0.height, outside())
    }
}

/**
 * 0.7.1: every touch of a window (SystemUI's shade window, the launcher's drag layer), observed and never
 * consumed, so Arona can look where Sensei touches — not only when the touch lands on her.
 */
object AronaGaze {
    private val listeners = CopyOnWriteArrayList<(Int, Float, Float) -> Unit>()

    @JvmStatic
    fun dispatch(e: MotionEvent) {
        if (listeners.isEmpty()) return
        val a = e.actionMasked
        if (a != MotionEvent.ACTION_DOWN && a != MotionEvent.ACTION_MOVE && a != MotionEvent.ACTION_UP &&
            a != MotionEvent.ACTION_CANCEL) return
        val x = e.rawX
        val y = e.rawY
        listeners.forEach { it(a, x, y) }
    }

    /** Main thread; returns the remove action. */
    fun add(listener: (Int, Float, Float) -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }
}
