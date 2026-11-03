package org.blueos.arona

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.blueos.arona.BlueOSSettings as K

/**
 * BlueOS 0.8: the one place that decides colours (approved mockup mockups/blueos-0.8-colors.html, page «Цвета»).
 * An element has a role, not a colour. A role's colour comes from the Monet palette the system has just made from the
 * wallpaper — every time, so any wallpaper, light/dark theme and «Яркие цвета» work, and elements that share a role
 * share the colour (0.7 had ~15 places that each picked their own tone: that's why the design kept breaking).
 *
 * Lock screen (light text over the wallpaper): TEXT white · TINT near-white with the wallpaper's hue (big digits) ·
 * ACCENT one detail (halo, «Сэнсэй») · SECOND / THIRD the palette's other colours · CUSTOM the user's own, the only
 * one that doesn't follow the wallpaper. AOD: neutral grey at the chosen brightness. Home: Material roles of the theme.
 *
 * 0.8.1: on the lock screen what is picked is what is drawn, exactly. 0.8 moved the halo's, the minutes' and the details'
 * tone up until they reached 3:1 on the wallpaper, so they turned white or not depending on the wallpaper under them and
 * on «Сила защиты» (the user: «будто рандомно»). Readability is the dimming's job (BlueOSLockProtection counts these colours).
 */
object BlueOSColorRoles {
    const val TEXT = 0
    const val TINT = 1
    const val ACCENT = 2
    const val SECOND = 3
    const val THIRD = 4
    const val CUSTOM = 5

    const val PROTECT_AUTO = 0
    const val PROTECT_ALWAYS = 1
    const val PROTECT_OFF = 2

    const val AOD_NEUTRAL = 0
    const val AOD_HALO = 1
    const val AOD_LOCK = 2

    const val ARONA_OUTLINE = 0
    const val ARONA_SHADOW = 1                              // hiding her = «Арона на AOD» off (blueos_arona_aod)

    const val LABELS_WALLPAPER = 0
    const val LABELS_WHITE = 1
    const val LABELS_PILL = 2

    /** The system's dark theme (not a context's: SystemUI's stays stale after a switch — 0.7.2). */
    @JvmStatic
    fun isDark(): Boolean =
        (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun role(ctx: Context, key: String) = K.int(ctx.contentResolver, key)

    // ------------------------------------------------------------------ HCT: Monet's hue / chroma / tone

    private fun hct(color: Int) = FloatArray(3).also { ColorUtils.colorToM3HCT(color, it) }
    private fun hct(h: Float, c: Float, t: Float) = ColorUtils.M3HCTToColor(h, c, t)

    /** Hue and chroma of the accent palette (its tone 50 holds the real chroma; tone 90 is clipped by sRGB). */
    private fun accent(ctx: Context) = hct(ctx.getColor(android.R.color.system_accent1_500))

    @JvmStatic
    fun custom(ctx: Context): Int = role(ctx, K.COLOR_CUSTOM) or OPAQUE

    @JvmStatic
    fun luminance(color: Int): Float = ColorUtils.calculateLuminance(color or OPAQUE).toFloat()

    @JvmStatic
    fun contrast(lumA: Float, lumB: Float): Float = (maxOf(lumA, lumB) + .05f) / (minOf(lumA, lumB) + .05f)

    private fun dim(color: Int, f: Float) =
        Color.rgb((Color.red(color) * f).toInt(), (Color.green(color) * f).toInt(), (Color.blue(color) * f).toInt())

    private const val OPAQUE = 0xFF000000.toInt()

    // ------------------------------------------------------------------ lock screen

    /** A lock screen role: light, over the wallpaper. */
    @JvmStatic
    fun lockColor(ctx: Context, role: Int): Int = when (role) {
        TEXT -> Color.WHITE
        TINT -> accent(ctx).let { hct(it[0], minOf(it[1], 14f), 94f) }
        SECOND -> ctx.getColor(android.R.color.system_accent2_200)
        THIRD -> ctx.getColor(android.R.color.system_accent3_200)
        CUSTOM -> custom(ctx)
        else -> ctx.getColor(android.R.color.system_accent1_200)
    }

    /** The colour [role] gives the element [key]: «Свой цвет» is the element's own (the shared one until it has one). */
    @JvmStatic
    fun lockRoleColor(ctx: Context, key: String, role: Int): Int {
        if (role == CUSTOM) K.int(ctx.contentResolver, key + K.COLOR_OWN).let { if (it != 0) return it or OPAQUE }
        return lockColor(ctx, role)
    }

    private fun lockElement(ctx: Context, key: String) = lockRoleColor(ctx, key, role(ctx, key))

    @JvmStatic fun lockDigits(ctx: Context) = lockElement(ctx, K.COLOR_LOCK_DIGITS)
    @JvmStatic fun lockText(ctx: Context) = lockElement(ctx, K.COLOR_LOCK_TEXT)
    @JvmStatic fun lockHalo(ctx: Context) = lockElement(ctx, K.COLOR_LOCK_HALO)
    @JvmStatic fun lockSensei(ctx: Context) = lockElement(ctx, K.COLOR_LOCK_SENSEI)
    @JvmStatic fun lockWeather(ctx: Context) = lockElement(ctx, K.COLOR_LOCK_WEATHER)
    @JvmStatic fun lockGlow(ctx: Context) = lockElement(ctx, K.COLOR_GLOW)
    /** 0.8: the lock clock's minutes in their own role; null = «Как цифры часов», the hours' colour. */
    @JvmStatic fun lockMinutes(ctx: Context): Int? =
        if (role(ctx, K.COLOR_LOCK_MINUTES) == SAME_AS_HOURS) null else lockElement(ctx, K.COLOR_LOCK_MINUTES)
    const val SAME_AS_HOURS = -1

    // ------------------------------------------------------------------ AOD

    @JvmStatic fun aodBrightness(ctx: Context) = role(ctx, K.COLOR_AOD_BRIGHT).coerceIn(25, 100) / 100f
    @JvmStatic fun aodMode(ctx: Context) = role(ctx, K.COLOR_AOD)

    /** Digits and text on AOD: grey — or, «Как на блокировке», their lock colour — at the AOD brightness. */
    @JvmStatic
    fun aodColor(ctx: Context, lockColor: Int) =
        dim(if (aodMode(ctx) == AOD_LOCK) lockColor else Color.WHITE, aodBrightness(ctx))

    @JvmStatic fun aodDigits(ctx: Context) = aodColor(ctx, lockDigits(ctx))
    @JvmStatic fun aodText(ctx: Context) = aodColor(ctx, lockText(ctx))
    @JvmStatic fun aodSensei(ctx: Context) = aodColor(ctx, lockSensei(ctx))
    @JvmStatic fun aodWeather(ctx: Context) = aodColor(ctx, lockWeather(ctx))

    @JvmStatic
    fun aodHalo(ctx: Context): Int = when (aodMode(ctx)) {
        AOD_HALO -> dim(accent(ctx).let { hct(it[0], minOf(it[1], 32f), 75f) }, aodBrightness(ctx))
        else -> aodColor(ctx, lockHalo(ctx))
    }

    @JvmStatic fun aodArona(ctx: Context) = role(ctx, K.COLOR_AOD_ARONA)

    /** Arona on AOD: a thin outline (few lit pixels) or a faint fill, grey at the AOD brightness. */
    @JvmStatic
    fun aodAronaColor(ctx: Context): Int {
        val c = dim(Color.WHITE, aodBrightness(ctx)) and 0x00FFFFFF
        return c or if (aodArona(ctx) == ARONA_SHADOW) 0x30000000 else 0xD0000000.toInt()
    }

    // ------------------------------------------------------------------ readability under light lock text

    @JvmStatic fun protect(ctx: Context) = role(ctx, K.COLOR_PROTECT)

    /**
     * How much to dim a wallpaper whose bright spots have luminance [p90] so that [fg] on it reaches the contrast
     * [target] (4.5 small text, 3 big digits) — 0 on dark wallpapers, never more than [cap] — times «Сила защиты»
     * (100 % = exactly that; above 100 % the cap grows with it, at most 0.92).
     */
    @JvmStatic
    fun protectAlpha(ctx: Context, p90: Float, fg: Int, target: Float, cap: Float): Float {
        val mode = protect(ctx)
        if (mode == PROTECT_OFF) return 0f
        val lim = (luminance(fg) + .05f) / target - .05f
        val a = if (p90 <= lim) 0f else 1f - maxOf(lim, 0f) / p90
        val s = K.int(ctx.contentResolver, K.COLOR_PROTECT_STR).coerceIn(0, 150) / 100f
        val max = maxOf(cap, minOf(cap * s, .92f))
        return minOf(max, (if (mode == PROTECT_ALWAYS) maxOf(a, .3f) else a) * s)
    }

    /**
     * 0.8.1: [protectAlpha] for the darkest of [colors], the colours really drawn on that part of the screen. A very dark
     * colour the user picked can't be saved by dimming the wallpaper under it: it doesn't count.
     */
    @JvmStatic
    fun protectAlpha(ctx: Context, p90: Float, colors: List<Int>, target: Float, cap: Float): Float =
        colors.filter { luminance(it) >= PROTECT_MIN_LUMINANCE }.maxOfOrNull { protectAlpha(ctx, p90, it, target, cap) } ?: 0f

    private const val PROTECT_MIN_LUMINANCE = .25f

    /** The dimming's colour: the palette's own deep tone (navy on blue wallpapers, wine on pink ones, never grey). */
    @JvmStatic fun protectTint(ctx: Context) = accent(ctx).let { hct(it[0], minOf(it[1], 30f), 8f) }

    // ------------------------------------------------------------------ home screen (Material roles)

    /** The one accent of a widget, with what goes on it and its container. */
    class Accent(@JvmField val color: Int, @JvmField val on: Int, @JvmField val container: Int, @JvmField val onContainer: Int)

    private fun sys(ctx: Context, dark: Boolean, light: Int, night: Int) = ctx.getColor(if (dark) night else light)

    @JvmStatic
    fun homeAccent(ctx: Context, dark: Boolean): Accent = when (role(ctx, K.COLOR_HOME_ACCENT)) {
        SECOND -> Accent(
            sys(ctx, dark, android.R.color.system_secondary_light, android.R.color.system_secondary_dark),
            sys(ctx, dark, android.R.color.system_on_secondary_light, android.R.color.system_on_secondary_dark),
            sys(ctx, dark, android.R.color.system_secondary_container_light, android.R.color.system_secondary_container_dark),
            sys(ctx, dark, android.R.color.system_on_secondary_container_light, android.R.color.system_on_secondary_container_dark))
        THIRD -> Accent(
            sys(ctx, dark, android.R.color.system_tertiary_light, android.R.color.system_tertiary_dark),
            sys(ctx, dark, android.R.color.system_on_tertiary_light, android.R.color.system_on_tertiary_dark),
            sys(ctx, dark, android.R.color.system_tertiary_container_light, android.R.color.system_tertiary_container_dark),
            sys(ctx, dark, android.R.color.system_on_tertiary_container_light, android.R.color.system_on_tertiary_container_dark))
        CUSTOM -> hct(custom(ctx)).let { (h, c) ->
            Accent(hct(h, c, if (dark) 80f else 40f), hct(h, c, if (dark) 20f else 100f),
                hct(h, c, if (dark) 30f else 90f), hct(h, c, if (dark) 90f else 10f))
        }
        else -> Accent(
            sys(ctx, dark, android.R.color.system_primary_light, android.R.color.system_primary_dark),
            sys(ctx, dark, android.R.color.system_on_primary_light, android.R.color.system_on_primary_dark),
            sys(ctx, dark, android.R.color.system_primary_container_light, android.R.color.system_primary_container_dark),
            sys(ctx, dark, android.R.color.system_on_primary_container_light, android.R.color.system_on_primary_container_dark))
    }

    /** The glyph on the icon plates. */
    @JvmStatic
    fun iconGlyph(ctx: Context, dark: Boolean): Int = when (role(ctx, K.COLOR_ICONS)) {
        TEXT -> sys(ctx, dark, android.R.color.system_on_surface_light, android.R.color.system_on_surface_dark)
        SECOND -> sys(ctx, dark, android.R.color.system_secondary_light, android.R.color.system_secondary_dark)
        THIRD -> sys(ctx, dark, android.R.color.system_tertiary_light, android.R.color.system_tertiary_dark)
        else -> homeAccent(ctx, dark).color
    }

    @JvmStatic fun labels(ctx: Context) = role(ctx, K.COLOR_LABELS)

    const val EQ_ACCENT = 0
    const val EQ_THIRD = 1
    const val EQ_TEXT = 2

    /** «Эквалайзер» in «Цвета»: the bars in the accent, the third palette colour or like text (widgets pick a theme resource). */
    @JvmStatic fun eq(ctx: Context) = role(ctx, K.COLOR_EQ)

    /**
     * 0.8: the same «Эквалайзер» role drawn right on the wallpaper — on the lock screen light like its text (the lock
     * roles), on the home screen the theme's own tones, like the widgets around it.
     */
    @JvmStatic
    fun eqOnWallpaper(ctx: Context, locked: Boolean): Int {
        if (locked) return when (eq(ctx)) {
            EQ_THIRD -> lockColor(ctx, THIRD)
            EQ_TEXT -> Color.WHITE
            else -> lockColor(ctx, ACCENT)
        }
        val dark = isDark()
        return when (eq(ctx)) {
            EQ_THIRD -> sys(ctx, dark, android.R.color.system_tertiary_light, android.R.color.system_tertiary_dark)
            EQ_TEXT -> sys(ctx, dark, android.R.color.system_on_surface_variant_light, android.R.color.system_on_surface_variant_dark)
            else -> sys(ctx, dark, android.R.color.system_primary_light, android.R.color.system_primary_dark)
        }
    }
}
