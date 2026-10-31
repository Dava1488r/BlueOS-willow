package org.blueos.arona

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Phone motion for live objects, one per process, sensors on only while someone visible holds it ([acquire]).
 *  - tilt: −1…1 left/right and forward/back, from gravity;
 *  - inertia: [jigX]/[jigY] (dp) — live objects lag behind the phone's motion on a damped spring and come back;
 *    how far = «Сила инерции» (4…40 dp), small motions are cut by a dead zone;
 *  - shake: enough strong direction changes within the last second («Чувствительность»: 8 × 1.8 g … 4 × 0.6 g),
 *    ignored while the step detector counts steps, at most once per 5 s;
 *  - fall: free fall → impact within 1 s («Чувствительность»: 220 ms / 3.0 g … 80 ms / 1.4 g; below 50 the phone
 *    must also lie still after the impact); ignored with the proximity sensor covered (pocket) or during a call.
 * 0.7.1: everything is measured in time, not in samples — the accelerometer comes at a different rate in
 * each process (that is why shaking worked on the lock screen but not in the launcher's widget).
 * Debug: `setprop log.tag.BlueOSMotion DEBUG` prints free falls, impacts and shakes.
 */
object BlueOSMotion : SensorEventListener, Choreographer.FrameCallback {
    interface Listener {
        fun onShake() {}
        fun onFall() {}
    }

    private const val TAG = "BlueOSMotion"
    private const val G = SensorManager.GRAVITY_EARTH
    private const val RING = 512

    /** Smoothed tilt, x: right edge down = +1, y: top away from you = +1. */
    val tilt = FloatArray(2)
    /** Inertia offset in dp (screen axes, y down). */
    var jigX = 0f; private set
    var jigY = 0f; private set

    private lateinit var app: Context
    private val listeners = mutableListOf<Listener>()
    private var sm: SensorManager? = null
    private val grav = FloatArray(3)
    private var linX = 0f
    private var linY = 0f
    private var tiltTX = 0f
    private var tiltTY = 0f
    private var vx = 0f
    private var vy = 0f
    private var boostUntil = 0L
    private var lastFrame = 0L
    private var lastStep = 0L
    private var lastShake = 0L
    private var lastFall = 0L
    private var near = false

    // the dominant-axis linear acceleration (g) with its time, for the shake detector
    private val ringV = FloatArray(RING)
    private val ringT = LongArray(RING)
    private var ringPos = 0

    // fall state machine
    private var freeFallSince = 0L
    private var freeFallMin = 9f
    private var impactDeadline = 0L
    private var impactPeak = 0f
    private var stillSince = 0L

    // settings, re-read twice a second (not per sensor event)
    private var cfgAt = 0L
    private var shakeOn = true
    private var shakeSteps = true
    private var shakeBig = true
    private var shakeSens = 50
    private var fallOn = true
    private var fallPocket = true
    private var fallSens = 70
    private var inertiaStr = 50

    private var clients = 0

    /** Main thread. Sensors run while at least one holder is visible; call the returned action to release. */
    fun acquire(context: Context, listener: Listener? = null): () -> Unit {
        app = context.applicationContext
        listener?.let { listeners += it }
        if (clients++ == 0) register()
        var released = false
        return {
            if (!released) {
                released = true
                listener?.let { listeners -= it }
                if (--clients == 0) unregister()
            }
        }
    }

    private fun register() {
        if (sm != null) return
        val m = app.getSystemService(SensorManager::class.java) ?: return
        sm = m
        cfgAt = 0L
        m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { m.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        m.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { m.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        m.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { m.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lastFrame = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun unregister() {
        sm?.unregisterListener(this)
        sm = null
        Choreographer.getInstance().removeFrameCallback(this)
        jigX = 0f; jigY = 0f; vx = 0f; vy = 0f
        freeFallSince = 0L; impactDeadline = 0L; stillSince = 0L
    }

    private fun config(now: Long) {
        if (now - cfgAt < 500) return
        cfgAt = now
        val cr = app.contentResolver
        shakeOn = BlueOSSettings.on(cr, BlueOSSettings.SHAKE)
        shakeSteps = BlueOSSettings.on(cr, BlueOSSettings.SHAKE_STEPS)
        shakeBig = BlueOSSettings.on(cr, BlueOSSettings.SHAKE_BIG)
        shakeSens = BlueOSSettings.int(cr, BlueOSSettings.SHAKE_SENS).coerceIn(0, 100)
        fallOn = BlueOSSettings.on(cr, BlueOSSettings.FALL)
        fallPocket = BlueOSSettings.on(cr, BlueOSSettings.FALL_POCKET)
        fallSens = BlueOSSettings.int(cr, BlueOSSettings.FALL_SENS).coerceIn(0, 100)
        inertiaStr = BlueOSSettings.int(cr, BlueOSSettings.INERTIA_STR).coerceIn(0, 100)
    }

    private val debug get() = Log.isLoggable(TAG, Log.DEBUG)

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(e: SensorEvent) {
        val now = SystemClock.uptimeMillis()
        when (e.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> lastStep = now
            Sensor.TYPE_PROXIMITY -> near = e.values[0] < e.sensor.maximumRange
            Sensor.TYPE_ACCELEROMETER -> accel(e.values, now)
        }
    }

    private fun accel(a: FloatArray, now: Long) {
        config(now)
        for (i in 0..2) grav[i] = grav[i] * 0.8f + a[i] * 0.2f
        // tilt from gravity (≈ ±30° = full scale), 40° forward is "straight" when holding a phone
        tiltTX = (-grav[0] / (G * 0.5f)).coerceIn(-1f, 1f)
        val pitch = Math.toDegrees(asin((grav[1] / G).coerceIn(-1f, 1f).toDouble())).toFloat()
        tiltTY = ((40f - pitch) / 30f).coerceIn(-1f, 1f)
        linX = (a[0] - grav[0]) / G
        linY = (a[1] - grav[1]) / G

        // shake
        ringV[ringPos] = if (abs(linX) > abs(linY)) linX else linY
        ringT[ringPos] = now
        ringPos = (ringPos + 1) % RING
        if (shakeOn && now - lastShake > 5000 && shakeNow(now)) {
            val walking = shakeSteps && now - lastStep < 2000
            if (debug) Log.d(TAG, "shake (sens $shakeSens)${if (walking) " ignored: steps" else ""}")
            if (!walking) {
                lastShake = now
                if (shakeBig) {
                    boostUntil = now + 1800
                    vx += (if (Math.random() < .5) -1f else 1f) * 260f
                    vy += (Math.random().toFloat() - .5f) * 200f
                }
                listeners.toList().forEach { it.onShake() }
            }
        }
        // fall
        if (fallOn) fall(sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]) / G, now)
    }

    /** ≥ need direction changes of ≥ thr g within the last second. */
    private fun shakeNow(now: Long): Boolean {
        val s = shakeSens / 100f
        val thr = 1.8f - 1.2f * s
        val need = (8f - 4f * s).roundToInt()
        var cross = 0
        var last = 0f
        for (i in 0 until RING) {
            val k = (ringPos + i) % RING                         // oldest first
            if (now - ringT[k] > 1000) continue
            val v = ringV[k]
            if (abs(v) < thr) continue
            val sg = sign(v)
            if (last != 0f && sg != last) cross++
            last = sg
        }
        return cross >= need
    }

    private fun fall(m: Float, now: Long) {
        val s = fallSens / 100f
        val ffThr = 0.3f + 0.25f * s                              // "weightless" below this, g
        val ffMin = (220f - 140f * s).toLong()                    // for at least this long, ms
        val hit = 3.0f - 1.6f * s                                 // then an impact of at least this, g
        val needStill = s < 0.5f                                  // low sensitivity: must lie still afterwards
        when {
            stillSince > 0 -> {
                if (abs(m - 1f) > 0.5f && now - stillSince > 300) {
                    if (debug) Log.d(TAG, "fall: moved again after the impact — not a drop")
                    stillSince = 0
                } else if (now - stillSince > 700) { stillSince = 0; fire(now) }
            }
            impactDeadline > 0 -> {
                impactPeak = maxOf(impactPeak, m)
                if (m >= hit) {
                    if (debug) Log.d(TAG, "fall: impact %.2f g (need %.2f)".format(m, hit))
                    impactDeadline = 0
                    if (needStill) stillSince = now else fire(now)
                } else if (now > impactDeadline) {
                    if (debug) Log.d(TAG, "fall: impact too soft, peak %.2f g (need %.2f)".format(impactPeak, hit))
                    impactDeadline = 0
                }
            }
            m < ffThr -> {
                if (freeFallSince == 0L) { freeFallSince = now; freeFallMin = m }
                freeFallMin = minOf(freeFallMin, m)
            }
            freeFallSince > 0 -> {
                val dur = now - freeFallSince
                freeFallSince = 0
                if (debug && dur >= 50) Log.d(TAG, "free fall %d ms, min %.2f g (need %d ms)".format(dur, freeFallMin, ffMin))
                if (dur >= ffMin) {
                    impactDeadline = now + 1000
                    impactPeak = m
                    if (m >= hit) { impactDeadline = 0; if (needStill) stillSince = now else fire(now) }
                }
            }
        }
    }

    private fun fire(now: Long) {
        if (now - lastFall < 2000) return
        val pocket = fallPocket && (near || app.getSystemService(AudioManager::class.java).mode == AudioManager.MODE_IN_CALL)
        if (debug) Log.d(TAG, "fall!${if (pocket) " ignored: pocket / call" else ""}")
        if (pocket) return
        lastFall = now
        listeners.toList().forEach { it.onFall() }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (sm == null) return
        val dt = if (lastFrame == 0L) 0.016f else ((frameTimeNanos - lastFrame) / 1e9f).coerceIn(0.001f, 0.05f)
        lastFrame = frameTimeNanos
        val k = 1f - kotlin.math.exp(-dt * 5f)
        tilt[0] += (tiltTX - tilt[0]) * k
        tilt[1] += (tiltTY - tilt[1]) * k
        // inertia spring; «Сила инерции» 0…100 → up to 4…40 dp, a smaller dead zone when stronger
        val s = inertiaStr / 100f
        val dead = 0.25f - 0.12f * s
        var ax = linX
        var ay = linY
        val mag = hypot(ax, ay)
        val f = if (mag > dead) (mag - dead) / mag else 0f
        ax *= f; ay *= f
        val boost = if (SystemClock.uptimeMillis() < boostUntil) 2.4f else 1f
        val gain = (2f + 16f * s) * boost
        val lim = (4f + 36f * s) * boost
        // the phone accelerates right/up → objects lag left/down (screen y is down, sensor y is up)
        vx += (-jigX * 140f - vx * 8f - ax * gain * 140f) * dt
        vy += (-jigY * 140f - vy * 8f + ay * gain * 140f) * dt
        jigX = (jigX + vx * dt).coerceIn(-lim, lim)
        jigY = (jigY + vy * dt).coerceIn(-lim, lim)
        Choreographer.getInstance().postFrameCallback(this)
    }
}

/**
 * 0.8: the music's spectrum for what draws it outside the widgets — the wallpaper's equalizer, the halo's pulse.
 * Filled in SystemUI (BlueOSMusicPulse, from the visualizer service) only while one of them is on, the screen is on
 * and music plays; smoothed like the widgets' bars (quick rise, slow fall). Readers may see a half-written frame — it's
 * only bar heights.
 */
object BlueOSBeat {
    const val BANDS = 32

    /** 0…1 per band, low to high. */
    @JvmField val shown = FloatArray(BANDS)

    /** Music is playing and someone listens. */
    @Volatile @JvmField var playing = false

    /** 0…1: the low end (kick, bass) — for pulses. */
    @Volatile @JvmField var energy = 0f

    /** The wallpaper wants to know where the lock screen's clock block ends («Волна под часами»). */
    @Volatile @JvmField var wantTopBlock = false

    /** Screen px: the lock screen's clock, greeting and date together; set by the lock screen while [wantTopBlock]. */
    @Volatile @JvmField var topBlock: android.graphics.RectF? = null

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<Runnable>()

    fun addListener(r: Runnable) { listeners += r }
    fun removeListener(r: Runnable) { listeners -= r }

    /** [playing] changed (or the bars fell back to rest). */
    fun changed() = listeners.forEach { it.run() }

    /** Still something to draw: playing, or the bars still falling. */
    val visible get() = playing || energy > 0.004f || shown.any { it > 0.004f }

    /** The shown level at [f] (0…1, low to high), between the bands. */
    fun level(f: Float): Float {
        val x = f.coerceIn(0f, 1f) * (BANDS - 1)
        val i = x.toInt()
        val j = minOf(BANDS - 1, i + 1)
        return shown[i] + (shown[j] - shown[i]) * (x - i)
    }
}

/**
 * 0.8: whether a live view is really on the screen. isShown stays true on another home screen page and under
 * «Все приложения» (the home screen only fades out): Arona on page 2 kept animating 60 times a second there, and every
 * frame of hers made the launcher redraw the page in view (and SurfaceFlinger re-blur every widget and icon) — idle,
 * the launcher drew 600 frames in 10 s. Main thread only.
 */
object BlueOSOnScreen {
    private val loc = IntArray(2)

    @JvmStatic
    fun visible(v: android.view.View): Boolean {
        if (!v.isShown || v.width == 0 || v.height == 0) return false
        val root = v.rootView
        v.getLocationInWindow(loc)
        if (loc[0] >= root.width || loc[0] + v.width <= 0 || loc[1] >= root.height || loc[1] + v.height <= 0) return false
        var p: android.view.View? = v
        while (p != null) {
            if (p.alpha < 0.004f) return false
            p = p.parent as? android.view.View
        }
        return true
    }

    /**
     * Moves [v] only by a visible amount: a new translation each frame — even 0.01 px from the spring — is a new frame
     * of the whole window. Back to exactly 0 always.
     */
    @JvmStatic
    fun nudge(v: android.view.View, x: Float, y: Float) {
        if (abs(v.translationX - x) >= MIN_PX || (x == 0f && v.translationX != 0f)) v.translationX = x
        if (abs(v.translationY - y) >= MIN_PX || (y == 0f && v.translationY != 0f)) v.translationY = y
    }

    /** Below a quarter of a pixel a move can't be seen. */
    const val MIN_PX = 0.25f
}
