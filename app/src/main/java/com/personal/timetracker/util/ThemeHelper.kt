package com.personal.timetracker.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

object ThemeHelper {
    /** Perceived brightness 0..1 */
    fun luminance(color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (0.2126 * r + 0.7152 * g + 0.0722 * b).toFloat()
    }

    fun onColor(bg: Int): Int =
        if (luminance(bg) > 0.55f) Color.parseColor("#1C1B1F") else Color.WHITE

    fun container(primary: Int, dark: Boolean): Int =
        ColorUtils.blendARGB(primary, if (dark) Color.BLACK else Color.WHITE, if (dark) 0.65f else 0.82f)

    fun surface(dark: Boolean): Int =
        if (dark) Color.parseColor("#121212") else Color.parseColor("#F7F9FC")

    fun surfaceCard(dark: Boolean): Int =
        if (dark) Color.parseColor("#1E1E1E") else Color.WHITE

    fun textPrimary(dark: Boolean): Int =
        if (dark) Color.parseColor("#E8EAED") else Color.parseColor("#1C1B1F")

    fun textSecondary(dark: Boolean): Int =
        if (dark) Color.parseColor("#A0A4AB") else Color.parseColor("#5F6368")

    fun outline(dark: Boolean): Int =
        if (dark) Color.parseColor("#3C4043") else Color.parseColor("#E0E3E7")

    fun applyButton(btn: MaterialButton, primary: Int, filled: Boolean = true) {
        if (filled) {
            btn.backgroundTintList = ColorStateList.valueOf(primary)
            btn.setTextColor(onColor(primary))
            btn.iconTint = ColorStateList.valueOf(onColor(primary))
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            btn.strokeColor = ColorStateList.valueOf(primary)
            btn.strokeWidth = 2
            btn.setTextColor(primary)
            btn.iconTint = ColorStateList.valueOf(primary)
        }
        btn.cornerRadius = 28
    }

    fun applyBottomNav(nav: BottomNavigationView, primary: Int, dark: Boolean) {
        val muted = textSecondary(dark)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(primary, muted)
        val csl = ColorStateList(states, colors)
        nav.itemIconTintList = csl
        nav.itemTextColor = csl
        nav.setBackgroundColor(surfaceCard(dark))
        // subtle top divider feel
        nav.elevation = 12f
    }

    fun applyCard(card: MaterialCardView, dark: Boolean, accent: Int? = null) {
        card.setCardBackgroundColor(surfaceCard(dark))
        card.radius = 20f
        card.cardElevation = if (dark) 2f else 4f
        card.strokeWidth = 1
        card.strokeColor = outline(dark)
        if (accent != null) {
            // left accent via content - handled by caller
        }
    }

    fun sectionTitle(ctx: Context, text: String, dark: Boolean, primary: Int): TextView {
        return TextView(ctx).apply {
            this.text = text
            textSize = 13f
            setTextColor(primary)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.04f
            setPadding(4, 28, 4, 10)
        }
    }

    fun pageTitle(ctx: Context, text: String, dark: Boolean): TextView {
        return TextView(ctx).apply {
            this.text = text
            textSize = 24f
            setTextColor(textPrimary(dark))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(4, 8, 4, 16)
        }
    }
}
