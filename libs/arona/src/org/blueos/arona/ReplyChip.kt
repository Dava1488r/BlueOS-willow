package org.blueos.arona

import android.content.Context
import android.content.res.Configuration
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Arona's reply: a Material 3 "rich tooltip" chip — surface container colour, 12 dp corners, an accent badge
 * with the emotion. Colours are Monet roles, so it follows any wallpaper and light/dark.
 * Lives in a FrameLayout host; [show] places it beside her head, never over her face or the [avoid] rects.
 */
class ReplyChip(context: Context) : LinearLayout(context) {
    private val dp = resources.displayMetrics.density
    private val badge = TextView(context)
    private val text = TextView(context)
    private val hide = Runnable { animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(150).withEndAction { visibility = GONE } }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = GONE
        elevation = 3 * dp
        val pad = (10 * dp).toInt()
        setPadding(pad, (7 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
        badge.gravity = Gravity.CENTER
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        badge.typeface = Typeface.DEFAULT_BOLD
        addView(badge, LayoutParams((20 * dp).toInt(), (20 * dp).toInt()).apply { marginEnd = (8 * dp).toInt() })
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        text.typeface = Typeface.create("google-sans-text-medium", Typeface.NORMAL)
        text.maxWidth = (200 * dp).toInt()
        addView(text)
        applyColors()
    }

    private fun color(id: Int) = context.getColor(id)

    fun applyColors() {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        background = GradientDrawable().apply {
            cornerRadius = 12 * dp
            setColor(color(if (dark) android.R.color.system_neutral1_800 else android.R.color.system_neutral1_100))
        }
        text.setTextColor(color(if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900))
        badge.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color(if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600))
        }
        badge.setTextColor(color(if (dark) android.R.color.system_accent1_800 else android.R.color.system_accent1_0))
    }

    /**
     * @param head her head in host coordinates; @param avoid rects that must stay visible (clock, halo…);
     * @param hostW/hostH the host's size; @param outside a rect the chip goes above or below (the widget's card).
     */
    fun show(line: AronaReplies.Line, kind: AronaReplies.Kind, head: RectF, avoid: List<RectF>, hostW: Int, hostH: Int,
             outside: RectF? = null) {
        val cr = context.contentResolver
        val sfxMode = BlueOSSettings.int(cr, BlueOSSettings.ARONA_VOICE) == 1
        val showEmo = sfxMode || kind == AronaReplies.Kind.NOTIFICATION
        val showText = BlueOSSettings.on(cr, BlueOSSettings.ARONA_TEXT) && !(sfxMode && BlueOSSettings.on(cr, BlueOSSettings.ARONA_EMO_ONLY))
        if (!showEmo && !showText) return
        applyColors()
        badge.visibility = if (showEmo) View.VISIBLE else View.GONE
        badge.text = line.emo
        text.visibility = if (showText) View.VISIBLE else View.GONE
        text.text = line.text(BlueOSSettings.lang(cr))
        (badge.layoutParams as LayoutParams).marginEnd = if (showText) (8 * dp).toInt() else 0

        removeCallbacks(hide)
        visibility = View.VISIBLE
        alpha = 0f
        val g = 6 * dp
        val margin = 8 * dp
        val top0 = 36 * dp                                         // below the status bar
        measure(MeasureSpec.makeMeasureSpec((hostW - 2 * margin).toInt().coerceAtLeast(1), MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(hostH, MeasureSpec.AT_MOST))
        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        val blocked = avoid + head
        // 0.7.1: every candidate is first pulled inside the host (a small host used to crash coerceIn), then
        // checked against what must stay visible; if nothing is free, the least covering place wins
        fun clampX(x: Float) = x.coerceAtMost(hostW - margin - w).coerceAtLeast(margin)
        fun clampY(y: Float) = y.coerceAtMost(hostH - margin - h).coerceAtLeast(top0)
        fun overlap(x: Float, y: Float): Float = blocked.sumOf { r ->
            val o = RectF(x, y, x + w, y + h)
            (if (o.intersect(r)) o.width() * o.height() else 0f).toDouble()
        }.toFloat()
        val cx = head.centerX() - w / 2
        // beside her head first, then lower beside her, then above / below her head, then around [outside]
        val ys = listOf(head.centerY() - h / 2, head.bottom + g, head.bottom + 2 * g + h, head.bottom + 3 * g + 2 * h)
        val raw = ys.flatMap { y -> listOf(floatArrayOf(head.left - w - g, y), floatArrayOf(head.right + g, y)) } +
            listOf(floatArrayOf(cx, head.top - h - g), floatArrayOf(cx, head.bottom + g)) +
            (outside?.let { listOf(floatArrayOf(cx, it.top - h - g), floatArrayOf(cx, it.bottom + g)) } ?: emptyList())
        val candidates = raw.map { floatArrayOf(clampX(it[0]), clampY(it[1])) }
        val c = candidates.firstOrNull { overlap(it[0], it[1]) == 0f } ?: candidates.minBy { overlap(it[0], it[1]) }
        // the host may lay the chip out with margins (the launcher's insets): position it absolutely
        translationX = c[0] - left
        translationY = c[1] - top
        pivotX = (head.centerX() - c[0]).coerceIn(0f, w)
        pivotY = if (c[1] > head.centerY()) 0f else h
        scaleX = 0.94f; scaleY = 0.94f
        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
        postDelayed(hide, if (showText) 2800 else 1500)
    }
}
