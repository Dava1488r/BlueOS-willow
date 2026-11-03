package org.blueos.arona

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.badlogic.gdx.files.FileHandle
import com.esotericsoftware.spine.SkeletonBinary
import com.esotericsoftware.spine.SkeletonData
import com.esotericsoftware.spine.Skin
import com.esotericsoftware.spine.android.AndroidAtlasAttachmentLoader
import com.esotericsoftware.spine.android.AndroidSkeletonDrawable
import com.esotericsoftware.spine.android.AndroidTextureAtlas
import com.esotericsoftware.spine.attachments.AttachmentLoader
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment
import com.esotericsoftware.spine.attachments.ClippingAttachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.PathAttachment
import com.esotericsoftware.spine.attachments.PointAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment
import com.esotericsoftware.spine.attachments.Sequence
import java.io.File
import java.util.concurrent.Executors

/**
 * The Spine models (4.2.33, from the game) on /product: Arona, Plana (0.7.2) and Arona at her desk (0.7.2, the
 * work page scene cut down by tools/cut-workpage.py). Each is loaded once per process off the main thread; every
 * view gets its own skeleton + animation state but shares the atlas bitmaps and data.
 */
object AronaModel {
    private const val TAG = "BlueOSArona"
    /** 0.8.1: on /product or added by the user (BlueOSMedia). */
    val DESK_DIR: String get() = BlueOSMedia.dir("spine/arona_desk", "arona_desk.skel")

    private class Model(val atlas: AndroidTextureAtlas, val data: SkeletonData)

    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "BlueOSAronaLoad").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private val models = HashMap<String, Model>()
    private val waiting = HashMap<String, MutableList<(AndroidSkeletonDrawable?) -> Unit>>()

    /** Main thread only. [onReady] gets a new drawable on the main thread, or null if the model is missing. */
    fun newDrawable(onReady: (AndroidSkeletonDrawable?) -> Unit) = newDrawable(Character.ARONA, onReady)

    fun newDrawable(c: Character, onReady: (AndroidSkeletonDrawable?) -> Unit) {
        // one character at a time: the other one's bitmaps (~16 MB) go once nobody draws her any more
        Character.values().filter { it != c }.forEach { models.remove(key(it.dir, it.model)) }
        load(c.dir, c.model, lenient = false, onReady)
    }

    /** Arona at her desk: only her and the desk are in the atlas, the rest of the scene's attachments are skipped. */
    fun newDesk(onReady: (AndroidSkeletonDrawable?) -> Unit) = load(DESK_DIR, "arona_desk", lenient = true, onReady)

    private fun key(dir: String, name: String) = "$dir/$name"

    private fun load(dir: String, name: String, lenient: Boolean, onReady: (AndroidSkeletonDrawable?) -> Unit) {
        val k = key(dir, name)
        models[k]?.let { onReady(AndroidSkeletonDrawable(it.atlas, it.data)); return }
        val list = waiting.getOrPut(k) { mutableListOf() }
        list += onReady
        if (list.size > 1) return                                         // already loading
        io.execute {
            val m = try {
                val atlas = AndroidTextureAtlas.fromFile(File(dir, "$name.atlas"))
                val loader: AttachmentLoader = AndroidAtlasAttachmentLoader(atlas).let { if (lenient) Lenient(it) else it }
                Model(atlas, SkeletonBinary(loader).readSkeletonData(FileHandle(File(dir, "$name.skel"))))
            } catch (t: Throwable) {
                Log.e(TAG, "Can't load $name from $dir", t)
                null
            }
            main.post {
                if (m != null) models[k] = m
                val callbacks = waiting.remove(k).orEmpty()
                callbacks.forEach { it(m?.let { AndroidSkeletonDrawable(it.atlas, it.data) }) }
            }
        }
    }

    /** Attachments whose image isn't in the atlas are left out instead of failing the whole model. */
    private class Lenient(private val base: AndroidAtlasAttachmentLoader) : AttachmentLoader {
        override fun newRegionAttachment(skin: Skin?, name: String?, path: String?, sequence: Sequence?): RegionAttachment? =
            try { base.newRegionAttachment(skin, name, path, sequence) } catch (e: RuntimeException) { null }
        override fun newMeshAttachment(skin: Skin?, name: String?, path: String?, sequence: Sequence?): MeshAttachment? =
            try { base.newMeshAttachment(skin, name, path, sequence) } catch (e: RuntimeException) { null }
        override fun newBoundingBoxAttachment(skin: Skin?, name: String?): BoundingBoxAttachment? =
            base.newBoundingBoxAttachment(skin, name)
        override fun newClippingAttachment(skin: Skin?, name: String?): ClippingAttachment? =
            base.newClippingAttachment(skin, name)
        override fun newPathAttachment(skin: Skin?, name: String?): PathAttachment? = base.newPathAttachment(skin, name)
        override fun newPointAttachment(skin: Skin?, name: String?): PointAttachment? = base.newPointAttachment(skin, name)
    }
}
