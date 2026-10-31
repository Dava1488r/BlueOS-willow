package org.blueos.arona

import android.content.ContentResolver

/**
 * 0.7.2: who lives on the lock screen and in the widget («Персонаж»). Plana (NP0035_spr) is the only other model
 * from the game with Arona's live rig (Touch_Eye_Key / Touch_Point_Key, Pat, blink, expressions); the students'
 * story sprites have no touch at all. Everything that differs between the two models is here.
 */
enum class Character(
    val id: Int,
    /** Spine model on /product: [dir]/[model].{atlas,skel,png}. */
    val dir: String,
    val model: String,
    /** Her voice lines, named as on the wiki without the character prefix (Work_In_1.ogg). */
    val voiceDir: String,
    /** The head bone (the chip beside it, what counts as her body). */
    val head: String,
    /** The mouth attachments' prefix and her open mouths small → wide (she speaks with her voice). */
    val mouthPrefix: String,
    val mouths: List<String>,
    val fullBody: AronaView.Viewport,
    val upperBody: AronaView.Viewport,
    /** Expressions: shaken (spiral eyes), a fall (shocked). */
    val dizzyFace: String,
    val startleFace: String,
    /** The work page scene has Arona sitting at her desk (the widget, «Садится на парту»); none for Plana. */
    val hasDesk: Boolean,
) {
    ARONA(0, "/product/media/blueos/spine/arona", "arona_spr", "/product/media/blueos/arona/voice", "Head",
        "Mouse_", listOf("Mouse_04", "Mouse_13", "Mouse_03"),
        AronaView.FULL_BODY, AronaView.UPPER_BODY, "30", "28", true),
    PLANA(1, "/product/media/blueos/spine/plana", "NP0035_spr", "/product/media/blueos/plana/voice", "Head_set",
        "Mouth_", listOf("Mouth_04", "Mouth_05", "Mouth_08"),
        // measured in spine-models (setup pose bounds; the eye bone at −24, 910 vs Arona's 3, 874)
        AronaView.Viewport(-620f, -848f, 1154f, 2216f), AronaView.Viewport(-357f, 186f, 660f, 1187f), "13", "10", false);

    /**
     * 0.8: her model is on /product. A public build has no files from the game (vendor/blueos/ASSETS.md: the builder
     * adds them); without any character everything about her stays off (BlueOSSettings: ARONA_SHOW reads 0).
     */
    val available: Boolean by lazy { java.io.File(dir, "$model.skel").isFile }

    companion object {
        /** The chosen one, or the other one if only she is there. */
        @JvmStatic
        fun current(cr: ContentResolver): Character {
            val chosen = if (BlueOSSettings.int(cr, BlueOSSettings.CHARACTER) == 1) PLANA else ARONA
            return if (chosen.available || !anyAvailable) chosen else entries.first { it.available }
        }

        /** Any character's files are on this build. */
        @JvmStatic
        val anyAvailable: Boolean by lazy { entries.any { it.available } }
    }
}
