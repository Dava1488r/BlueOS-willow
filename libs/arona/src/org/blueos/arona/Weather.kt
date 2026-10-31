package org.blueos.arona

import android.content.ContentResolver
import android.provider.Settings
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Current weather, written to Settings.Secure [BlueOSSettings.WEATHER] as JSON by SystemUI's
 * BlueOSWeather (Open-Meteo): {"t":7.2,"code":3,"hi":9.1,"lo":3.0,"feels":4.0,"day":1,"ts":…}.
 */
data class Weather(
    val temp: Double,
    val code: Int,
    val high: Double,
    val low: Double,
    val feels: Double,
    val day: Boolean,
    val time: Long,
) {
    enum class Kind { CLEAR, CLOUDY, RAIN, SNOW, STORM }

    /** WMO weather code → our five kinds. */
    val kind: Kind
        get() = when (code) {
            0, 1 -> Kind.CLEAR
            in 51..67, in 80..82 -> Kind.RAIN
            in 71..77, 85, 86 -> Kind.SNOW
            in 95..99 -> Kind.STORM
            else -> Kind.CLOUDY
        }

    /** Material Symbols icon for this weather. */
    val icon: Int
        get() = when (kind) {
            Kind.CLEAR -> if (day) R.drawable.blueos_wx_clear else R.drawable.blueos_wx_clear_night
            // 0.8: «малооблачно» had a sun at night too (only «ясно» had a night icon)
            Kind.CLOUDY -> when (code) {
                2 -> if (day) R.drawable.blueos_wx_partly else R.drawable.blueos_wx_partly_night
                45, 48 -> R.drawable.blueos_wx_fog
                else -> R.drawable.blueos_wx_cloudy
            }
            Kind.RAIN -> R.drawable.blueos_wx_rain
            Kind.SNOW -> R.drawable.blueos_wx_snow
            Kind.STORM -> R.drawable.blueos_wx_storm
        }

    /** Fresh enough to talk about (not older than 3 h). */
    val fresh get() = System.currentTimeMillis() - time < 3 * 3600_000L

    companion object {
        @JvmStatic
        fun read(cr: ContentResolver): Weather? = try {
            Settings.Secure.getString(cr, BlueOSSettings.WEATHER)?.let { s ->
                val j = JSONObject(s)
                Weather(j.getDouble("t"), j.getInt("code"), j.optDouble("hi", j.getDouble("t")),
                    j.optDouble("lo", j.getDouble("t")), j.optDouble("feels", j.getDouble("t")),
                    j.optInt("day", 1) == 1, j.optLong("ts", 0))
            }
        } catch (e: Exception) {
            null
        }

        /** "7°" or "45°" in Fahrenheit if the user chose it. */
        @JvmStatic
        fun format(cr: ContentResolver, celsius: Double): String {
            val v = if (BlueOSSettings.on(cr, BlueOSSettings.WEATHER_F)) celsius * 9 / 5 + 32 else celsius
            return "${v.roundToInt()}°"
        }
    }
}
