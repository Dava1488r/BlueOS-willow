package org.blueos.arona

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Picture
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import com.badlogic.gdx.math.Vector2
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.android.AndroidSkeletonDrawable
import com.esotericsoftware.spine.attachments.Attachment
import com.esotericsoftware.spine.android.SkeletonRenderer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Live Arona (Spine). The interaction is Blue Archive's own Memorial Lobby mechanics: the model has two
 * "touch keys" (Touch_Eye_Key — eyes/face, Touch_Point_Key — face/fringe/head) that the game moves toward the
 * finger after the animations are applied, while tracks 1/2 play Look_01_A/M or Pat_01_A/M; on release
 * LookEnd/PatEnd. Sleep = the model's calm closed-eyes expression "99" held on top + a drooping head.
 *
 * Draws only while attached, shown and not [frozen] (AOD = frozen, optionally as a [silhouette]).
 */
class AronaView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs), Choreographer.FrameCallback {

    /** World rectangle shown (Spine units, y up). Default = full body incl. umbrella and halo. */
    data class Viewport(val x: Float, val y: Float, val w: Float, val h: Float)

    var viewport = FULL_BODY
        set(value) { field = value; invalidate() }

    /**
     * 0.7.2: Arona or Plana. Changing it loads the other model; the viewport follows (her full body / upper body).
     * [onReady] is called again when the new model is there (the host re-lays her out).
     */
    var character = Character.ARONA
        set(value) {
            if (field == value) return
            val upper = viewport == field.upperBody
            field = value
            viewport = if (upper) value.upperBody else value.fullBody
            mouthSlot = null; mouths = null; mouthRest = null; talkT = -1f
            load()
        }

    /** Tilt (−1…1 each, from [BlueOSMotion]) she follows with her eyes; null = off. */
    var tilt: (() -> FloatArray?)? = null

    /** Swaying to music: slow and smooth, deliberately not on the beat. */
    var music = false

    /** AOD: no animation; draws the last pose once, as a flat silhouette if [silhouette] != null. */
    var frozen = false
        set(value) { field = value; updateRunning(); invalidate() }
    var silhouette: Int? = null
        set(value) {
            field = value
            silhouettePaint.colorFilter = value?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
            invalidate()
        }

    /** 0.8: the silhouette as a thin outline only (AOD: far fewer lit pixels than a fill). */
    var outline = false
        set(value) { field = value; invalidate() }

    /**
     * 0.8: how far she has turned into the [silhouette] (0 = in colour, 1 = silhouette only) — the host animates it
     * so going to AOD is a cross-fade, not a cut (the faint AOD shadow over a still light wallpaper looked like
     * she vanished for a moment).
     */
    var silhouetteMix = 1f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }
    private val fadePaint = Paint()

    var onReady: (() -> Unit)? = null

    val isReady get() = drawable != null
    var sleeping = false
        private set
    val touchMode get() = mode

    private var drawable: AndroidSkeletonDrawable? = null
    private val renderer = SkeletonRenderer()
    private val silhouettePaint = Paint()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastFrame = 0L
    private var time = 0f

    private var mode = NONE
    private var targetX = 0f
    private var targetY = 0f
    private val eye = Vector2()
    private val point = Vector2()
    private val tmp = Vector2()
    private var joltY = 0f
    private var joltV = 0f

    private var loadToken = 0                         // (declared before init: load() runs from there)

    init {
        load()
    }

    private fun load() {
        val c = character
        val token = ++loadToken
        AronaReplies.warmUp(c)                         // her voices' loudness, for the mouth (0.7.1)
        AronaModel.newDrawable(c) { d ->
            if (token != loadToken) return@newDrawable           // the character changed meanwhile
            drawable = d ?: return@newDrawable
            mode = NONE
            d.animationState.setAnimation(0, "Idle_01", true)
            if (sleeping) applySleep(true)
            updateRunning()
            invalidate()
            onReady?.invoke()
        }
    }

    /** Plays [name] on [track] if this model has it (Plana has no Look_01_A), else clears the track. */
    private fun play(track: Int, name: String, loop: Boolean) {
        val d = drawable ?: return
        if (d.skeleton.data.findAnimation(name) != null) d.animationState.setAnimation(track, name, loop)
        else d.animationState.setEmptyAnimation(track, 0.1f)
    }

    // ---------------------------------------------------------------- interaction API

    fun begin(kind: Int, wx: Float, wy: Float) {
        val st = drawable?.animationState ?: return
        mode = kind
        targetX = wx
        targetY = wy
        st.setEmptyAnimation(TRACK_BLINK, 0f)
        if (sleeping) st.setEmptyAnimation(TRACK_SLEEP, 0.1f)
        play(1, if (kind == PAT) "Pat_01_A" else "Look_01_A", true)
        play(2, if (kind == PAT) "Pat_01_M" else "Look_01_M", true)
    }

    fun moveTo(wx: Float, wy: Float) {
        targetX = wx
        targetY = wy
    }

    fun end() {
        val st = drawable?.animationState ?: return
        if (mode == NONE) return
        val pat = mode == PAT
        play(1, if (pat) "PatEnd_01_A" else "LookEnd_01_A", false)
        play(2, if (pat) "PatEnd_01_M" else "LookEnd_01_M", false)
        st.addEmptyAnimation(1, 0.2f, 0f)
        st.addEmptyAnimation(2, 0.2f, 0f)
        mode = NONE
        if (sleeping) applySleep(true)
    }

    fun setSleeping(on: Boolean) {
        if (sleeping == on) return
        sleeping = on
        applySleep(on)
    }

    // ---------------------------------------------------------------- face (0.7.1)

    private var faceToken = 0
    private var faceOn = false

    /** Shows one of the model's expressions ("00"…"32") for [ms], then her own face again. */
    fun face(name: String?, ms: Long) {
        val d = drawable ?: return
        if (name == null || d.skeleton.data.findAnimation(name) == null) return
        val token = ++faceToken
        faceOn = true
        crossfade(FACE_FADE_S)
        d.animationState.setAnimation(TRACK_FACE, name, true).mixDuration = 0.15f
        handler.postDelayed({
            if (token == faceToken) {
                faceOn = false
                crossfade(FACE_FADE_S)
                d.animationState.setEmptyAnimation(TRACK_FACE, 0.3f)
            }
        }, ms)
    }

    // ---------------------------------------------------------------- soft changes (0.8)

    /**
     * 0.8: the model's eyes and mouths are separate pictures — Spine can only switch them, never blend, so a reply's
     * face came and went in one frame. Here the frame before the switch is kept (a [Picture]: the same draw calls,
     * nothing rendered twice) and fades out over the new one for [seconds]. The body is the same in both, so only
     * the face really cross-fades.
     */
    private var fadePic: Picture? = null
    private var fadeT = -1f
    private var fadeLen = 0.25f

    private fun crossfade(seconds: Float) {
        val d = drawable ?: return
        if (!running || width == 0 || height == 0 || (silhouette != null && silhouetteMix > 0f)) return
        val p = Picture()
        drawFigure(p.beginRecording(width, height), d, asSilhouette = false)
        p.endRecording()
        fadePic = p
        fadeLen = seconds
        fadeT = 0f
    }

    /** Fall: a little hop and a wide-eyed look up for a moment. */
    fun startle() {
        val sk = drawable?.skeleton ?: return
        val wasSleeping = sleeping
        if (wasSleeping) setSleeping(false)
        face(character.startleFace, 1400)
        joltV = 520f
        val h = sk.findBone("Touch_Eye") ?: return
        begin(LOOK, h.worldX, h.worldY + 400f)
        handler.postDelayed({ end(); if (wasSleeping) setSleeping(true) }, 900)
    }

    /** Shake: her eyes roll around for a moment ("голова кругом"). */
    fun dizzy() {
        val sk = drawable?.skeleton ?: return
        if (mode != NONE) return
        val wasSleeping = sleeping
        if (wasSleeping) setSleeping(false)
        val h = sk.findBone("Touch_Eye") ?: return
        val cx = h.worldX
        val cy = h.worldY
        face(character.dizzyFace, 1600)                      // spiral eyes
        begin(LOOK, cx, cy)
        dizzyUntil = time + 1.3f
        dizzyCenter.set(cx, cy)
        dizzyWasSleeping = wasSleeping
    }

    private var dizzyUntil = 0f
    private val dizzyCenter = Vector2()
    private var dizzyWasSleeping = false

    // ---------------------------------------------------------------- talking (0.7.1)

    private var talkEnv: FloatArray? = null
    private var talkLen = 0f
    private var talkT = -1f
    private var mouthLevel = 0f
    private var mouthSlot: Slot? = null
    private var mouthRest: Attachment? = null
    /** The model's open mouths, small → wide: Mouse_04 «o», Mouse_13 half open, Mouse_03 open. */
    private var mouths: List<Attachment>? = null

    /**
     * Her voice line started: the mouth follows its loudness ([env]: 0…1 every [AronaReplies.ENV_MS] ms, from the
     * voice file itself; null = not measured yet → a plain speaking rhythm) for [durationMs].
     */
    fun talk(env: FloatArray?, durationMs: Int) {
        talkEnv = env
        talkLen = durationMs / 1000f
        talkT = 0f
    }

    private fun mouth(sk: Skeleton, dt: Float) {
        if (talkT < 0f) return
        val slot = mouthSlot ?: findMouth(sk) ?: run { talkT = -1f; return }
        val open = mouths ?: return
        val cur = slot.attachment
        if (cur !in open) mouthRest = cur                    // what the animation / expression wants now
        talkT += dt
        if (talkT >= talkLen) {
            talkT = -1f
            mouthLevel = 0f
            slot.attachment = mouthRest
            return
        }
        val env = talkEnv
        val target = if (env != null && env.isNotEmpty()) {
            env[((talkT * 1000f / AronaReplies.ENV_MS).toInt()).coerceIn(0, env.size - 1)]
        } else {
            (0.5f + 0.5f * sin(talkT * 2f * Math.PI.toFloat() * 4.5f)) * (0.6f + 0.4f * sin(talkT * 7.3f))
        }
        // opens fast, closes a bit slower — like speech (0.8: a softer, held version read worse — the user wanted this back)
        mouthLevel += (target - mouthLevel) * (1f - exp(-dt * if (target > mouthLevel) 40f else 18f))
        slot.attachment = when {
            mouthLevel < 0.18f -> mouthRest
            mouthLevel < 0.42f -> open[0]
            mouthLevel < 0.7f -> open[1]
            else -> open[2]
        }
    }

    private fun findMouth(sk: Skeleton): Slot? {
        val c = character
        val slot = sk.slots.firstOrNull { it.attachment?.name?.startsWith(c.mouthPrefix) == true }
            ?: sk.slots.firstOrNull { it.data.name.contains("Mouse", true) || it.data.name.contains("Mouth", true) }
            ?: return null
        val list = c.mouths.mapNotNull { sk.getAttachment(slot.data.index, it) }
        if (list.size < 3) return null
        mouthSlot = slot
        mouths = list
        mouthRest = slot.attachment
        return slot
    }

    fun isOnHead(wx: Float, wy: Float): Boolean {
        val h = drawable?.skeleton?.findBone("Touch_Point") ?: return false
        return hypot(wx - h.worldX, wy - h.worldY) < 260f
    }

    /** True if (x, y) in view pixels is on her (head or body), so touches elsewhere can pass through. */
    fun isOnBody(x: Float, y: Float): Boolean {
        val sk = drawable?.skeleton ?: return false
        val w = toWorld(x, y)
        val head = sk.findBone(character.head) ?: return false
        if (hypot(w[0] - head.worldX, w[1] - (head.worldY + 70f)) < 300f) return true
        return abs(w[0] - sk.x) < 230f && w[1] > character.fullBody.y && w[1] < head.worldY
    }

    /** View pixels → Spine world. */
    fun toWorld(x: Float, y: Float): FloatArray {
        val s = scale()
        return floatArrayOf(viewport.x + (x - offsetX(s)) / s, viewport.y + (height - y) / s)
    }

    /**
     * 0.8: a still picture without the frame loop (the widget picker's preview): her idle pose [seconds] in.
     * False = the model isn't loaded in this process yet.
     */
    fun still(seconds: Float = 0.6f): Boolean {
        if (drawable == null) return false
        var t = 0f
        while (t < seconds) { step(0.05f); t += 0.05f }
        return true
    }

    /** Her head as a rectangle in this view's pixels (for placing the reply chip beside it). */
    fun headRect(out: RectF): RectF {
        val head = drawable?.skeleton?.findBone(character.head)
        val s = scale()
        val wx = head?.worldX ?: 0f
        val wy = (head?.worldY ?: 780f) + 70f
        val cx = offsetX(s) + (wx - viewport.x) * s
        val cy = height - (wy - viewport.y) * s
        val r = 230f * s
        out.set(cx - r, cy - r * 1.1f, cx + r, cy + r * 0.9f)
        return out
    }

    // ---------------------------------------------------------------- animation

    private fun applySleep(on: Boolean) {
        val st = drawable?.animationState ?: return
        if (on) {
            st.setEmptyAnimation(TRACK_BLINK, 0f)
            st.setAnimation(TRACK_SLEEP, "99", true)
            st.timeScale = 0.6f
        } else {
            st.setEmptyAnimation(TRACK_SLEEP, 0.15f)
            st.timeScale = 1f
        }
    }

    private val blink = object : Runnable {
        override fun run() {
            val st = drawable?.animationState
            if (st != null && running && mode == NONE && !sleeping && !faceOn) {
                st.setAnimation(TRACK_BLINK, "Eye_Close_01", false)
                st.addEmptyAnimation(TRACK_BLINK, 0.05f, 0.05f)
            }
            handler.postDelayed(this, (2500 + Math.random() * 3500).toLong())
        }
    }

    private fun step(dt: Float) {
        val d = drawable ?: return
        val sk = d.skeleton
        val st = d.animationState
        time += dt
        joltV += (-joltY * 160f - joltV * 9f) * dt
        joltY += joltV * dt
        val sway = if (music && !sleeping) 1f else 0f
        val phase = time * 2f * Math.PI.toFloat() / 3.6f

        if (dizzyUntil > 0f) {
            if (time >= dizzyUntil) {
                dizzyUntil = 0f
                end()
                if (dizzyWasSleeping) setSleeping(true)
            } else {
                targetX = dizzyCenter.x + cos(time * 11f) * 220f
                targetY = dizzyCenter.y + sin(time * 11f) * 140f
            }
        }

        if (fadeT >= 0f) {
            fadeT += dt / fadeLen
            if (fadeT >= 1f) { fadeT = -1f; fadePic = null }
        }
        st.update(dt)
        st.apply(sk)
        mouth(sk, dt)
        sk.x = sway * sin(phase) * 7f
        sk.y = joltY
        sk.update(dt)
        sk.updateWorldTransform(Skeleton.Physics.update)

        var ex = 0f; var ey = 0f; var qx = 0f; var qy = 0f
        if (mode != NONE) {
            val anchor = sk.findBone(if (mode == PAT) "Touch_Point" else "Touch_Eye")
            if (anchor != null) {
                var dx = targetX - anchor.worldX
                var dy = targetY - anchor.worldY
                val r = if (mode == PAT) 120f else 160f
                val len = hypot(dx, dy)
                if (len > r) { dx *= r / len; dy *= r / len }
                if (mode == PAT) { qx = dx; qy = dy * 0.35f } else { ex = dx; ey = dy }
            }
        } else {
            val t = tilt?.invoke()
            if (t != null && !sleeping) { ex = t[0] * 110f; ey = -t[1] * 70f }
            if (sleeping) { qx = 25f; qy = -55f }
            else if (sway > 0f) { qx = sin(phase + 0.6f) * 60f; qy = -abs(cos(phase)) * 10f }
        }
        val k = 1f - exp(-dt * if (mode != NONE) 10f else 3.5f)
        eye.x += (ex - eye.x) * k; eye.y += (ey - eye.y) * k
        point.x += (qx - point.x) * k; point.y += (qy - point.y) * k
        moveKey(sk, "Touch_Eye_Key", eye)
        moveKey(sk, "Touch_Point_Key", point)
        sk.updateWorldTransform(Skeleton.Physics.pose)
    }

    private fun moveKey(sk: Skeleton, name: String, offset: Vector2) {
        val b = sk.findBone(name) ?: return
        val p = b.parent ?: return
        tmp.set(p.worldX + offset.x, p.worldY + offset.y)
        p.worldToLocal(tmp)
        b.x = tmp.x
        b.y = tmp.y
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        // 0.8: off the screen (another home screen page, under «Все приложения») she holds her pose: no frames
        if (!BlueOSOnScreen.visible(this)) {
            lastFrame = 0L
            Choreographer.getInstance().postFrameCallback(this)
            return
        }
        val dt = if (lastFrame == 0L) 0f else ((frameTimeNanos - lastFrame) / 1e9f).coerceIn(0f, 0.05f)
        lastFrame = frameTimeNanos
        step(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun updateRunning(detaching: Boolean = false) {
        val want = !detaching && isAttachedToWindow && isShown && !frozen && drawable != null
        if (want == running) return
        running = want
        if (want) {
            lastFrame = 0L
            Choreographer.getInstance().postFrameCallback(this)
            handler.postDelayed(blink, 3000)
        } else {
            Choreographer.getInstance().removeFrameCallback(this)
            handler.removeCallbacks(blink)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateRunning() }
    override fun onDetachedFromWindow() { updateRunning(detaching = true); super.onDetachedFromWindow() }
    override fun onVisibilityAggregated(isVisible: Boolean) { super.onVisibilityAggregated(isVisible); updateRunning() }

    // ---------------------------------------------------------------- drawing

    private fun scale() = if (width == 0 || height == 0) 1f else min(width / viewport.w, height / viewport.h)
    private fun offsetX(s: Float) = (width - viewport.w * s) / 2f

    override fun onDraw(canvas: Canvas) {
        val d = drawable ?: return
        val mix = if (silhouette != null) silhouetteMix else 0f
        if (mix < 1f) {
            // in colour (fading out while she turns into the silhouette)
            fadePaint.alpha = ((1f - mix) * 255).toInt()
            val l = if (mix > 0f) canvas.saveLayer(null, fadePaint) else canvas.save()
            drawFigure(canvas, d, asSilhouette = false)
            // 0.8: the frame before a face / mouth switch, fading out over the new one
            val pic = fadePic
            if (pic != null && fadeT in 0f..1f) {
                val f = canvas.saveLayerAlpha(null, ((1f - fadeT) * 255).toInt())
                canvas.drawPicture(pic)
                canvas.restoreToCount(f)
            }
            canvas.restoreToCount(l)
            if (mix <= 0f) return
        }
        silhouettePaint.alpha = (mix * 255).toInt()
        drawFigure(canvas, d, asSilhouette = true)
    }

    private fun drawFigure(canvas: Canvas, d: AndroidSkeletonDrawable, asSilhouette: Boolean) {
        val s = scale()
        val layer = if (asSilhouette) canvas.saveLayer(null, silhouettePaint) else canvas.save()
        val commands = renderer.render(d.skeleton)
        fun pose(dx: Float, dy: Float) {
            canvas.save()
            canvas.translate(offsetX(s) + dx, height.toFloat() + dy)
            canvas.scale(s, -s)
            canvas.translate(-viewport.x, -viewport.y)
            renderer.renderToCanvas(canvas, commands)
            canvas.restore()
        }
        if (asSilhouette && outline) {
            // the figure shifted around a small circle, then the figure itself cut out: only a ring is left
            val r = 1.5f * resources.displayMetrics.density
            for (i in 0 until 12) {
                val a = i * Math.PI / 6
                pose((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
            }
            val cut = canvas.saveLayer(null, cutPaint)
            pose(0f, 0f)
            canvas.restoreToCount(cut)
        } else {
            pose(0f, 0f)
        }
        canvas.restoreToCount(layer)
    }

    private val cutPaint = Paint().apply { xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    companion object {
        const val NONE = 0
        const val LOOK = 1
        const val PAT = 2
        private const val TRACK_BLINK = 3
        private const val TRACK_SLEEP = 4
        private const val TRACK_FACE = 5
        /** 0.8: a face (expression) comes and goes as a soft cross-fade. */
        private const val FACE_FADE_S = 0.28f
        val FULL_BODY = Viewport(-510f, -795f, 1011f, 2128f)
        val UPPER_BODY = Viewport(-330f, 150f, 660f, 1187f)
    }
}
