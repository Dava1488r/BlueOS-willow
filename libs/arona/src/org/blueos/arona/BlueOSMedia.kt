package org.blueos.arona

import android.os.SystemClock
import java.io.File

/**
 * 0.8.1: where the characters' files are. A build made with the game's files has them on /product; a public build
 * (no files from the game) gets them from the user — Settings → «Файлы персонажа» (BlueOSAssetsActivity) copies them
 * to /data/misc/blueos in the same layout (sepolicy blueos_media_file: Settings writes, SystemUI, Launcher3 and
 * Wallpaper & style read). /product wins when both have a file.
 */
object BlueOSMedia {
    const val PRODUCT = "/product/media/blueos"
    const val ADDED = "/data/misc/blueos"

    /** [rel] on /product if [probe] (a file in it, or the folder itself) is there, else the user's copy. */
    @JvmStatic
    fun dir(rel: String, probe: String? = null): String =
        if (exists("$PRODUCT/$rel", probe)) "$PRODUCT/$rel" else "$ADDED/$rel"

    /** [probe] in [rel] on /product or in the user's copy. */
    @JvmStatic
    fun has(rel: String, probe: String): Boolean = exists("$PRODUCT/$rel", probe) || exists("$ADDED/$rel", probe)

    // asked often (BlueOSSettings reads «is there a character» with every setting) — a file check every 2 s is plenty
    private val cache = HashMap<String, Pair<Long, Boolean>>()

    private fun exists(dir: String, probe: String?): Boolean {
        val key = "$dir/${probe ?: ""}"
        val now = SystemClock.uptimeMillis()
        synchronized(cache) { cache[key]?.let { (at, v) -> if (now - at < 2000) return v } }
        val v = if (probe == null) File(dir).isDirectory else File(dir, probe).isFile
        synchronized(cache) { cache[key] = now to v }
        return v
    }
}
