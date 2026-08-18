package com.personal.timetracker.util

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Consistent, nicer-looking dialogs for the whole app: a colored icon badge + title/subtitle
 * header, a divider, a scrollable body, and theme-aware rounded background — replacing the
 * plain default AlertDialog look used previously.
 */
object DialogHelper {

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    /** Styled outlined text field matching the app's rounded look. */
    fun inputField(
        ctx: Context,
        hint: String,
        value: String = "",
        primary: Int,
        number: Boolean = false,
        multiline: Boolean = false
    ): Pair<TextInputLayout, TextInputEditText> {
        val edit = TextInputEditText(ctx).apply {
            setText(value)
            if (number) inputType = InputType.TYPE_CLASS_NUMBER
            if (multiline) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 2
                gravity = Gravity.TOP
            }
        }
        val til = TextInputLayout(
            ctx, null, com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            this.hint = hint
            setBoxCornerRadii(dp(ctx, 16).toFloat(), dp(ctx, 16).toFloat(), dp(ctx, 16).toFloat(), dp(ctx, 16).toFloat())
            boxStrokeColor = primary
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(ctx, 14) }
            addView(edit)
        }
        return til to edit
    }

    fun sectionLabel(ctx: Context, text: String, dark: Boolean): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 12f
        setTextColor(ThemeHelper.textSecondary(dark))
        setPadding(dp(ctx, 4), dp(ctx, 14), dp(ctx, 4), dp(ctx, 2))
    }

    private fun header(
        ctx: Context, icon: String, title: String, subtitle: String?, primary: Int, dark: Boolean
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 26), dp(ctx, 24), dp(ctx, 26), dp(ctx, 16))
        }
        val badge = TextView(ctx).apply {
            text = icon
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(ThemeHelper.onColor(primary))
            background = ThemeHelper.glossy(primary, dark, oval = true)
            elevation = dp(ctx, 3).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 46), dp(ctx, 46)).apply {
                marginEnd = dp(ctx, 14)
            }
        }
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(ctx).apply {
            text = title
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ThemeHelper.textPrimary(dark))
        })
        if (!subtitle.isNullOrBlank()) {
            col.addView(TextView(ctx).apply {
                text = subtitle
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark))
                setPadding(0, dp(ctx, 2), 0, 0)
            })
        }
        row.addView(badge)
        row.addView(col)
        return row
    }

    /**
     * Show a styled dialog. [onPositive] returns true to allow the dialog to close, or false
     * to keep it open (e.g. after a validation failure / Toast).
     */
    fun show(
        ctx: Context,
        icon: String,
        title: String,
        subtitle: String? = null,
        primary: Int,
        dark: Boolean,
        body: View,
        positiveText: String = "ذخیره",
        negativeText: String? = "انصراف",
        neutralText: String? = null,
        onNeutral: (() -> Unit)? = null,
        onPositive: () -> Boolean
    ): AlertDialog {
        val outer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        outer.addView(header(ctx, icon, title, subtitle, primary, dark))
        outer.addView(View(ctx).apply {
            setBackgroundColor(ThemeHelper.outline(dark))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1))
        })
        val scroll = ScrollView(ctx)
        val bodyPad = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 26), dp(ctx, 6), dp(ctx, 26), dp(ctx, 4))
            addView(body)
        }
        scroll.addView(bodyPad)
        outer.addView(scroll)

        val builder = AlertDialog.Builder(ctx)
            .setView(outer)
            .setPositiveButton(positiveText, null)
        if (negativeText != null) builder.setNegativeButton(negativeText, null)
        if (neutralText != null) builder.setNeutralButton(neutralText, null)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(GradientDrawable().apply {
            cornerRadius = dp(ctx, 26).toFloat()
            setColor(ThemeHelper.surfaceCard(dark))
        })
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(primary)
                setOnClickListener { if (onPositive()) dialog.dismiss() }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ThemeHelper.textSecondary(dark))
            if (neutralText != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
                    setTextColor(ThemeHelper.textSecondary(dark))
                    setOnClickListener { onNeutral?.invoke(); dialog.dismiss() }
                }
            }
        }
        dialog.show()
        return dialog
    }

    /** Small styled confirm dialog for destructive actions (delete, etc). */
    fun confirm(
        ctx: Context,
        icon: String = "⚠",
        title: String,
        message: String,
        primary: Int,
        dark: Boolean,
        confirmText: String = "حذف",
        danger: Boolean = true,
        onConfirm: () -> Unit
    ) {
        val accent = if (danger) 0xFFE53935.toInt() else primary
        val body = TextView(ctx).apply {
            text = message
            textSize = 13.5f
            setTextColor(ThemeHelper.textSecondary(dark))
            setPadding(0, 0, 0, dp(ctx, 12))
        }
        show(
            ctx = ctx,
            icon = icon,
            title = title,
            primary = accent,
            dark = dark,
            body = body,
            positiveText = confirmText,
            onPositive = { onConfirm(); true }
        )
    }
}
