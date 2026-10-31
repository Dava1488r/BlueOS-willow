package org.blueos.arona

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Every BlueOS 0.7 switch lives in Settings.Secure (written by «Обои и стиль», read by SystemUI and
 * the launcher). One place for the keys and their defaults = the approved mockup's defaults.
 */
object BlueOSSettings {
    // Arona on the lock screen
    const val ARONA_SHOW = "blueos_arona_show"
    const val ARONA_MODE = "blueos_arona_mode"               // 0 clock, 1 fingerprint, 2 lobby, 3 dynamic, 4 custom
    const val ARONA_CUSTOM = "blueos_arona_custom"           // "cx;top;h" as fractions of the screen
    const val ARONA_LOOK = "blueos_arona_look"
    const val ARONA_PAT = "blueos_arona_pat"
    const val ARONA_VOICE = "blueos_arona_voice"             // 0 voice, 1 emotion SFX, 2 silent
    const val ARONA_TEXT = "blueos_arona_text"
    const val ARONA_LANG = "blueos_arona_lang"               // "ru", "en", "ja"
    const val ARONA_EMO_ONLY = "blueos_arona_emo_only"
    const val ARONA_NOTIF = "blueos_arona_notif"
    const val ARONA_SLEEP = "blueos_arona_sleep"
    const val ARONA_AOD = "blueos_arona_aod"
    const val ARONA_TILT = "blueos_arona_tilt"
    const val ARONA_GAZE = "blueos_arona_gaze"               // 0.7.1: looks where you touch, anywhere on the screen
    const val CHARACTER = "blueos_character"                 // 0.7.2: 0 Arona, 1 Plana (lock screen and widget)
    const val ARONA_DESK = "blueos_arona_desk"               // 0.7.2: in the widget she sits on a desk when left alone
    const val ARONA_DESK_VOICE = "blueos_arona_desk_voice"   // 0.7.2: …and hums out loud then (0 = a chip only)
    const val ARONA_LOBBY_NOTIFS = "blueos_arona_lobby_notifs" // 0.8: lobby + notifications: 0 as usual, 1 hidden,
                                                             // 2 a tap decides who's in front (her / the notifications)

    // Live halo clock
    const val HALO_LIVE = "blueos_halo_live"
    const val HALO_GLOW = "blueos_halo_glow"
    const val HALO_FLOAT = "blueos_halo_float"
    const val HALO_SPARK = "blueos_halo_spark"
    const val HALO_CHARGE = "blueos_halo_charge"
    const val HALO_TOUCH = "blueos_halo_touch"
    const val HALO_TILT = "blueos_halo_tilt"
    // 0.7.2: the lines under the lock clock — 0 as before (Android's), 1 like the Summary widget
    const val LOCK_GREETING_STYLE = "blueos_lock_greeting_style"   // semibold, «Сэнсэй» in the clock's colour
    const val LOCK_DATE_STYLE = "blueos_lock_date_style"           // medium, the weather in the clock's colour
    // 0 by the theme (dark theme → light clock, halo, greeting, date), 1 by the wallpaper under them (0.5–0.7.1),
    // 2 always light (default: the user doesn't want them dark in the light theme either)
    const val LOCK_TEXT_COLOR = "blueos_lock_text_color"

    // Phone motion
    const val FALL = "blueos_fall"
    const val FALL_ARONA = "blueos_fall_arona"
    const val FALL_HALO = "blueos_fall_halo"
    const val FALL_POCKET = "blueos_fall_pocket"
    const val FALL_LOW = "blueos_fall_low"                   // 0.7; 0.7.1: a preset of FALL_SENS (1 → 20)
    const val FALL_SENS = "blueos_fall_sens"                 // 0…100
    const val INERTIA = "blueos_inertia"
    const val INERTIA_SOFT = "blueos_inertia_soft"           // 0.7; 0.7.1: a preset of INERTIA_STR (1 → 15)
    const val INERTIA_STR = "blueos_inertia_str"             // 0…100
    const val SHAKE = "blueos_shake"
    const val SHAKE_ARONA = "blueos_shake_arona"
    const val SHAKE_BIG = "blueos_shake_big"
    const val SHAKE_STEPS = "blueos_shake_steps"
    const val SHAKE_SENS = "blueos_shake_sens"               // 0…100

    // Weather
    const val WEATHER = "blueos_weather"                     // JSON written by SystemUI's BlueOSWeather
    const val WEATHER_F = "blueos_weather_f"
    const val WEATHER_LOCK = "blueos_weather_lock"           // 0 icon + temperature, 1 temperature, 2 off
    const val WEATHER_SUMMARY = "blueos_weather_summary"
    const val WEATHER_DATE = "blueos_weather_date"
    const val WEATHER_LIVE = "blueos_weather_live"
    const val WEATHER_TALK = "blueos_weather_talk"

    // Home screen
    const val WIDGET_MATERIAL = "blueos_widget_material"     // 0 matte, 1 glass, 2 solid, 3 outline, 4 accent
    const val ICON_MATERIAL = "blueos_icon_material"         // -1 like widgets, else as above (0 = solid plate)
    const val ICON_ROUNDNESS = "blueos_icon_roundness"       // 0 square … 100 circle
    const val WIDGET_BLUR = "blueos_widget_blur"             // 0…100: blur of the wallpaper under the widget cards
    const val WIDGET_TRANSPARENCY = "blueos_widget_transparency" // 0…100 (0.6), every material but solid since 0.7.2
    const val ICON_TRANSPARENCY = "blueos_icon_transparency" // 0.7.2: 0…100, the icon plates
    const val LIVE_WIDGETS = "blueos_live_widgets"
    const val LIVE_SAVER = "blueos_live_saver"
    const val SUMMARY_CARD = "blueos_summary_card"
    const val SUMMARY_SHIMMER = "blueos_summary_shimmer"
    const val CLOCK_HALO = "blueos_clock_halo"
    const val CLOCK_SECONDS = "blueos_clock_seconds"
    const val MUSIC_LIVE = "blueos_music_live"
    const val DATE_SHIMMER = "blueos_date_shimmer"
    const val TOGGLES_LIVE = "blueos_toggles_live"
    const val ARONA_MUSIC = "blueos_arona_music"
    const val HOME_INERTIA = "blueos_home_inertia"
    const val HOME_TILT = "blueos_home_tilt"
    const val TILT_GLARE = "blueos_tilt_glare"
    const val TILT_HALO = "blueos_tilt_halo"
    const val TILT_ARONA = "blueos_tilt_arona"
    const val ARONA_WIDGET_TEXT = "blueos_arona_widget_text" // 0 name + status + hint, 1 status only, 2 no text
    const val ARONA_WIDGET_PHRASES = "blueos_arona_widget_phrases"   // the status changes over the day
    // 0.8: the «Эквалайзер: плеер» widget (default = as before)
    const val EQ_PLAYER_SKIP = "blueos_eq_player_skip"       // 1: back / forward next to play / pause
    const val EQ_PLAYER_STYLE = "blueos_eq_player_style"     // 0 bars from the middle line, 1 wave
    // 0.8: the wallpaper itself, «Цвета» → «Обои»: blur 0…100 % (of 50 dp), dimming 0…80 % black (0 = as before);
    // the lock screen has no dimming of its own — «Защита читаемости» does it there
    const val HOME_WALL_BLUR = "blueos_home_wall_blur"
    const val HOME_WALL_DIM = "blueos_home_wall_dim"
    // All drawn by the wallpaper itself (SystemUI BlueOSWallpaperWeather: blurred once, the weather on top).
    // 0: home screen only (the lock screen takes LOCK_WALL_BLUR), 1: always — the lock screen as blurred as home
    const val HOME_WALL_BLUR_MODE = "blueos_home_wall_blur_mode"
    const val LOCK_WALL_BLUR = "blueos_lock_wall_blur"
    // 0.8: live weather on the wallpaper (SystemUI ImageWallpaper): on/off and how strong, 10…100 %
    const val WALL_WEATHER = "blueos_wall_weather"
    const val WALL_WEATHER_STR = "blueos_wall_weather_str"
    // 0.8: the live equalizer on the wallpaper itself, not a widget (SystemUI, the wallpaper draws it under the widgets)
    const val WALL_EQ = "blueos_wall_eq"                     // 0 off, 1 home screen, 2 lock screen, 3 both
    const val WALL_EQ_STYLE = "blueos_wall_eq_style"         // 0 bars along the bottom, 1 a wave along the bottom, 2 a glow from below
    const val WALL_EQ_LOCK_POS = "blueos_wall_eq_lock_pos"   // lock screen: 0 like home (the bottom), 1 a wave under the clock
    const val WALL_EQ_STR = "blueos_wall_eq_str"             // 20…100 %: how high and how bright
    const val HALO_BEAT = "blueos_halo_beat"                 // 1: the lock clock's halo pulses with the music

    /** 0.8: the wallpaper blur of [key] in px (0 = none) — the same scale as «Размытие под виджетами». */
    @JvmStatic fun wallBlurPx(c: android.content.Context, key: String): Int =
        (int(c.contentResolver, key).coerceIn(0, 100) / 100f * 50f * c.resources.displayMetrics.density).toInt()

    /** 0.8: the wallpaper dimming of [key], 0…0.8 alpha of black. */
    @JvmStatic fun wallDim(cr: ContentResolver, key: String): Float = int(cr, key).coerceIn(0, 80) / 100f

    // 0.8 «Цвета» (Обои и стиль): a ROLE per element, resolved by BlueOSColorRoles (roles: 0 text, 1 tint,
    // 2 accent, 3 second, 4 third palette colour, 5 custom)
    const val COLOR_PROTECT = "blueos_color_protect"         // 0 auto, 1 always, 2 off — dimming under light lock text
    const val COLOR_PROTECT_STR = "blueos_color_protect_str" // 0…150 %: 100 = exactly what the contrast needs
    const val COLOR_LOCK_DIGITS = "blueos_color_lock_digits"
    const val COLOR_LOCK_HALO = "blueos_color_lock_halo"
    const val COLOR_LOCK_MINUTES = "blueos_color_lock_minutes" // 0.8: -1 like the hours, else a role (default accent)
    const val COLOR_LOCK_SENSEI = "blueos_color_lock_sensei"
    const val COLOR_LOCK_TEXT = "blueos_color_lock_text"     // greeting and date: text or tint
    const val COLOR_LOCK_WEATHER = "blueos_color_lock_weather"
    const val COLOR_GLOW = "blueos_color_glow"               // the live halo's glow
    const val COLOR_AOD = "blueos_color_aod"                 // 0 neutral, 1 halo in the accent, 2 like the lock screen
    const val COLOR_AOD_BRIGHT = "blueos_color_aod_bright"   // 25…100 %
    const val COLOR_AOD_ARONA = "blueos_color_aod_arona"     // 0 outline, 1 faint fill, 2 hidden
    const val COLOR_HOME_ACCENT = "blueos_color_home_accent" // the widgets' one accent: accent / second / third / custom
    const val COLOR_ICONS = "blueos_color_icons"             // the icons' glyph: accent / text / second / third
    const val COLOR_LABELS = "blueos_color_labels"           // 0 by the wallpaper (dark on light ones), 1 always white, 2 pill
    const val COLOR_EQ = "blueos_color_eq"                   // 0 accent, 1 accent → third, 2 like text
    const val COLOR_CUSTOM = "blueos_color_custom"           // ARGB of «Свой цвет»
    @JvmField val COLOR_KEYS = listOf(COLOR_PROTECT, COLOR_PROTECT_STR, COLOR_LOCK_DIGITS, COLOR_LOCK_MINUTES, COLOR_LOCK_HALO, COLOR_LOCK_SENSEI, COLOR_LOCK_TEXT,
        COLOR_LOCK_WEATHER, COLOR_GLOW, COLOR_AOD, COLOR_AOD_BRIGHT, COLOR_AOD_ARONA, COLOR_HOME_ACCENT, COLOR_ICONS,
        COLOR_LABELS, COLOR_EQ, COLOR_CUSTOM)

    // 0.8 (2026-10-30): the firmware defaults = the user's own settings of that day (the old value in the comment);
    // a fresh phone also gets them written once by SystemUI's BlueOSDefaults (some apps read keys with defaults of
    // their own). Wall equalizer: «Свечение снизу» on both screens.
    private val DEFAULTS: Map<String, Int> = mapOf(
        ARONA_SHOW to 1, ARONA_MODE to 2 /* 3 */, ARONA_LOOK to 1, ARONA_PAT to 1, ARONA_VOICE to 0,
        ARONA_TEXT to 1, ARONA_EMO_ONLY to 0, ARONA_NOTIF to 0 /* 1 */, ARONA_SLEEP to 0 /* 1 */, ARONA_AOD to 1, ARONA_TILT to 1,
        ARONA_GAZE to 1, CHARACTER to 1 /* 0 */, ARONA_DESK to 1, ARONA_DESK_VOICE to 1, ARONA_LOBBY_NOTIFS to 2 /* 0 */,
        HALO_LIVE to 1, HALO_GLOW to 1, HALO_FLOAT to 1, HALO_SPARK to 1, HALO_CHARGE to 1, HALO_TOUCH to 1, HALO_TILT to 1,
        LOCK_GREETING_STYLE to 1, LOCK_DATE_STYLE to 1, LOCK_TEXT_COLOR to 2,
        FALL to 1, FALL_ARONA to 1, FALL_HALO to 1, FALL_POCKET to 1, FALL_LOW to 0, FALL_SENS to 61 /* 70 */,
        INERTIA to 1, INERTIA_SOFT to 0, INERTIA_STR to 100 /* 50 */,
        SHAKE to 1, SHAKE_ARONA to 1, SHAKE_BIG to 1, SHAKE_STEPS to 1, SHAKE_SENS to 95 /* 50 */,
        WEATHER_F to 0, WEATHER_LOCK to 0, WEATHER_SUMMARY to 1, WEATHER_DATE to 1, WEATHER_LIVE to 1, WEATHER_TALK to 1,
        WIDGET_MATERIAL to 0, ICON_MATERIAL to -2 /* -1 */, ICON_ROUNDNESS to 86 /* 100 */, WIDGET_BLUR to 38 /* 40 */,
        WIDGET_TRANSPARENCY to 40 /* 15 */, ICON_TRANSPARENCY to 6 /* 0 */,
        LIVE_WIDGETS to 1, LIVE_SAVER to 1, SUMMARY_CARD to 1 /* 0 */, SUMMARY_SHIMMER to 0 /* 1 */, CLOCK_HALO to 1, CLOCK_SECONDS to 0 /* 1 */,
        MUSIC_LIVE to 1, DATE_SHIMMER to 1, TOGGLES_LIVE to 1, ARONA_MUSIC to 1,
        HOME_INERTIA to 1, HOME_TILT to 1, TILT_GLARE to 1, TILT_HALO to 1, TILT_ARONA to 1,
        // live weather on the wallpaper: off on a fresh phone (the user, 2026-10-30: still too heavy for a default)
        ARONA_WIDGET_TEXT to 0, ARONA_WIDGET_PHRASES to 1, WALL_WEATHER to 0 /* 1 */, WALL_WEATHER_STR to 70 /* 80 */,
        WALL_EQ to 3 /* 0 */, WALL_EQ_STYLE to 2 /* 0 */, WALL_EQ_LOCK_POS to 0, WALL_EQ_STR to 60, HALO_BEAT to 1 /* 0 */,
        EQ_PLAYER_SKIP to 1 /* 0 */, EQ_PLAYER_STYLE to 0, HOME_WALL_BLUR to 28 /* 0 */,
        COLOR_PROTECT to 0, COLOR_PROTECT_STR to 100, COLOR_LOCK_DIGITS to 3 /* 1 */, COLOR_LOCK_MINUTES to 2, COLOR_LOCK_HALO to 2, COLOR_LOCK_SENSEI to 2, COLOR_LOCK_TEXT to 0,
        COLOR_LOCK_WEATHER to 2 /* 0 */, COLOR_GLOW to 2, COLOR_AOD to 0, COLOR_AOD_BRIGHT to 80, COLOR_AOD_ARONA to 1 /* 0 */,
        COLOR_HOME_ACCENT to 2, COLOR_ICONS to 2, COLOR_LABELS to 0, COLOR_EQ to 0, COLOR_CUSTOM to 0xFF128AFA.toInt(),
    )

    /** 0.8: [DEFAULTS] that a fresh phone gets written into Settings (BlueOSDefaults): every key in the table. */
    @JvmStatic fun defaults(): Map<String, Int> = DEFAULTS
    /** «Своё место» of the user's character (mode 2 doesn't use it; it's where «Своё место» starts). */
    const val DEFAULT_ARONA_CUSTOM = "0.4706;0.2816;0.7202"

    @JvmStatic fun default(key: String): Int = DEFAULTS[key] ?: 0

    /** From Java: BlueOSSettings.getInt(cr, key) (`int` is a Java keyword). */
    @JvmStatic @JvmName("getInt")
    fun int(cr: ContentResolver, key: String): Int {
        // 0.8: a build without the game's files (public sources, vendor/blueos/ASSETS.md) has no character: she is
        // off everywhere — no lobby on the lock screen, nothing waiting for a model that never loads
        if (key == ARONA_SHOW && !Character.anyAvailable) return 0
        // 0.7.1: the 0.7 switches «пониже чувствительность» / «мягче» became presets of the sliders
        val def = when (key) {
            FALL_SENS -> if (Settings.Secure.getInt(cr, FALL_LOW, 0) != 0) 20 else default(key)
            INERTIA_STR -> if (Settings.Secure.getInt(cr, INERTIA_SOFT, 0) != 0) 15 else default(key)
            else -> default(key)
        }
        return Settings.Secure.getInt(cr, key, def)
    }

    @JvmStatic @JvmName("isOn")
    fun on(cr: ContentResolver, key: String): Boolean = int(cr, key) != 0

    fun string(cr: ContentResolver, key: String, def: String): String =
        Settings.Secure.getString(cr, key) ?: def

    fun lang(cr: ContentResolver): String = string(cr, ARONA_LANG, "ru")

    /** Calls [onChange] on the main thread whenever one of [keys] changes; returns the unregister action. */
    fun observe(context: Context, keys: Collection<String>, onChange: () -> Unit): () -> Unit {
        val cr = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = onChange()
        }
        keys.forEach { cr.registerContentObserver(Settings.Secure.getUriFor(it), false, observer) }
        return { cr.unregisterContentObserver(observer) }
    }
}
