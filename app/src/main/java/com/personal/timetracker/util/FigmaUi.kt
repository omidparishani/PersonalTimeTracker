package com.personal.timetracker.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

/**
 * سیستم طراحی یکپارچه مطابق Figma «رهگیر زمان شخصی».
 * همه صفحات باید از همین API استفاده کنند تا ظاهر یکنواخت بماند.
 */
object FigmaUi {
    /** رنگ اصلی قالب — همیشه آبی، نه سبز */
    const val PRIMARY = 0xFF1565C0.toInt()
    const val PRIMARY_SOFT = 0xFFE3F2FD.toInt()
    const val PRIMARY_SOFT_DARK = 0xFF1A3A5C.toInt()

    const val BG_LIGHT = 0xFFEEF2F7.toInt()
    const val BG_DARK = 0xFF0D1B2A.toInt()
    const val CARD_LIGHT = 0xFFFFFFFF.toInt()
    const val CARD_DARK = 0xFF1B2838.toInt()
    const val TEXT_LIGHT = 0xFF1A1A2E.toInt()
    const val TEXT_DARK = 0xFFE8EAED.toInt()
    const val MUTED_LIGHT = 0xFF6B7280.toInt()
    const val MUTED_DARK = 0xFF9AA0A6.toInt()
    const val SUCCESS = 0xFF1565C0.toInt() // وضعیت فعال هم آبی قالب
    const val SUCCESS_BG = 0xFFE3F2FD.toInt()
    const val SUCCESS_BG_DARK = 0xFF1A3A5C.toInt()
    const val WARNING = 0xFFEF6C00.toInt()
    const val DANGER = 0xFFE53935.toInt()
    /** سازگاری با کد قدیمی */
    const val TEXT = TEXT_LIGHT
    const val MUTED = MUTED_LIGHT
    const val BG = BG_LIGHT

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun bg(dark: Boolean) = if (dark) BG_DARK else BG_LIGHT
    fun card(dark: Boolean) = if (dark) CARD_DARK else CARD_LIGHT
    fun text(dark: Boolean) = if (dark) TEXT_DARK else TEXT_LIGHT
    fun muted(dark: Boolean) = if (dark) MUTED_DARK else MUTED_LIGHT
    fun softPrimary(dark: Boolean) = if (dark) PRIMARY_SOFT_DARK else PRIMARY_SOFT
    fun successBg(dark: Boolean) = if (dark) SUCCESS_BG_DARK else SUCCESS_BG

    fun rounded(color: Int, radiusDp: Float, ctx: Context): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * ctx.resources.displayMetrics.density
        }

    fun oval(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    /** کارت یکنواخت همه صفحات */
    fun whiteCard(ctx: Context, dark: Boolean = false): MaterialCardView {
        return MaterialCardView(ctx).apply {
            radius = dp(ctx, 16).toFloat()
            cardElevation = if (dark) 1f else 2f
            setCardBackgroundColor(card(dark))
            strokeWidth = if (dark) 1 else 0
            strokeColor = if (dark) 0xFF2A3A4A.toInt() else 0x00000000
            useCompatPadding = false
        }
    }

    /** اعمال قالب کارت روی کارت موجود (تسک‌ها و …) */
    fun styleCard(card: MaterialCardView, ctx: Context, dark: Boolean) {
        card.radius = dp(ctx, 16).toFloat()
        card.cardElevation = if (dark) 1f else 2f
        card.setCardBackgroundColor(card(dark))
        card.strokeWidth = if (dark) 1 else 0
        card.strokeColor = if (dark) 0xFF2A3A4A.toInt() else 0x00000000
        card.useCompatPadding = false
        val m = dp(ctx, 4)
        val lp = (card.layoutParams as? ViewGroup.MarginLayoutParams)
            ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { card.layoutParams = it }
        lp.setMargins(m, m, m, m)
    }

    fun screenHeader(
        ctx: Context,
        title: String,
        subtitle: String? = null,
        leftEmoji: String? = null,
        dark: Boolean = false,
        onLeft: (() -> Unit)? = null
    ): LinearLayout {
        // ارتفاع ثابت همه صفحات — عنوان راست، آیکن چپ
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            // هدر چسبیده به بالا + گوشه گرد پایین
            background = GradientDrawable().apply {
                setColor(PRIMARY)
                val r = dp(ctx, 20).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
            setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 18))
            minimumHeight = dp(ctx, 72)
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                // LTR تا آیکن همیشه سمت چپ بصری بماند
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }
            // آیکن چپ
            if (leftEmoji != null) {
                row.addView(
                    circleIcon(ctx, leftEmoji, onLeft),
                    LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)).apply {
                        marginEnd = dp(ctx, 12)
                    }
                )
            } else {
                row.addView(View(ctx), LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)).apply {
                    marginEnd = dp(ctx, 12)
                })
            }
            // عنوان راست‌چین
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(ctx).apply {
                text = title
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.END
                textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            })
            if (!subtitle.isNullOrBlank()) {
                col.addView(TextView(ctx).apply {
                    text = subtitle
                    textSize = 11f
                    setTextColor(0xCCFFFFFF.toInt())
                    gravity = Gravity.END
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                    setPadding(0, dp(ctx, 2), 0, 0)
                })
            }
            row.addView(col)
            addView(row)
        }
    }

    fun circleIcon(ctx: Context, emoji: String, onClick: (() -> Unit)? = null): TextView {
        return TextView(ctx).apply {
            text = emoji
            textSize = 18f
            gravity = Gravity.CENTER
            background = oval(0x33FFFFFF)
            setTextColor(Color.WHITE)
            if (onClick != null) setOnClickListener { onClick() }
        }
    }

    fun searchBar(ctx: Context, hint: String, dark: Boolean = false): EditText {
        return EditText(ctx).apply {
            this.hint = hint
            setSingleLine()
            textSize = 14f
            setTextColor(text(dark))
            setHintTextColor(muted(dark))
            setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
            background = rounded(card(dark), 16f, ctx)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
    }

    fun sectionLabel(
        ctx: Context,
        title: String,
        action: String? = null,
        onAction: (() -> Unit)? = null,
        dark: Boolean = false
    ): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 4), dp(ctx, 8), dp(ctx, 4), dp(ctx, 8))
            if (action != null) {
                addView(TextView(ctx).apply {
                    text = action
                    textSize = 12f
                    setTextColor(PRIMARY)
                    setOnClickListener { onAction?.invoke() }
                })
            }
            addView(TextView(ctx).apply {
                text = title
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(text(dark))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    fun listRow(
        ctx: Context,
        title: String,
        subtitle: String?,
        trailing: String?,
        dotColor: Int = PRIMARY,
        dark: Boolean = false
    ): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 4), dp(ctx, 12), dp(ctx, 4), dp(ctx, 12))
            if (trailing != null) {
                addView(TextView(ctx).apply {
                    text = trailing
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(text(dark))
                    setPadding(0, 0, dp(ctx, 12), 0)
                })
            }
            val mid = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            mid.addView(TextView(ctx).apply {
                text = title
                textSize = 13f
                setTextColor(text(dark))
                gravity = Gravity.END
            })
            if (!subtitle.isNullOrBlank()) {
                mid.addView(TextView(ctx).apply {
                    text = subtitle
                    textSize = 11f
                    setTextColor(muted(dark))
                    gravity = Gravity.END
                })
            }
            addView(mid)
            addView(View(ctx).apply {
                background = oval(dotColor)
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 10), dp(ctx, 10)).apply {
                    marginStart = dp(ctx, 10)
                }
            })
        }
    }

    fun settingsRow(
        ctx: Context,
        title: String,
        subtitle: String,
        iconEmoji: String,
        dark: Boolean = false,
        trailing: View? = null,
        onClick: (() -> Unit)? = null
    ): MaterialCardView {
        val card = whiteCard(ctx, dark)
        card.radius = 0f
        card.cardElevation = 0f
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 14), dp(ctx, 14), dp(ctx, 14), dp(ctx, 14))
            if (onClick != null) setOnClickListener { onClick() }
        }
        if (trailing != null) {
            row.addView(trailing)
        } else {
            row.addView(TextView(ctx).apply {
                text = "‹"
                textSize = 18f
                setTextColor(muted(dark))
                setPadding(dp(ctx, 4), 0, dp(ctx, 8), 0)
            })
        }
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(ctx).apply {
            text = title
            textSize = 14f
            setTextColor(text(dark))
            gravity = Gravity.END
        })
        texts.addView(TextView(ctx).apply {
            text = subtitle
            textSize = 11f
            setTextColor(muted(dark))
            gravity = Gravity.END
        })
        row.addView(texts)
        row.addView(TextView(ctx).apply {
            text = iconEmoji
            textSize = 16f
            gravity = Gravity.CENTER
            background = oval(softPrimary(dark))
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)).apply {
                marginStart = dp(ctx, 12)
            }
        })
        card.addView(row)
        return card
    }

    fun primaryButton(ctx: Context, label: String): com.google.android.material.button.MaterialButton {
        return com.google.android.material.button.MaterialButton(ctx).apply {
            text = label
            setBackgroundColor(PRIMARY)
            setTextColor(Color.WHITE)
            cornerRadius = dp(ctx, 14)
            elevation = 6f
        }
    }

    fun matchParent(bottom: Int = 0, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = bottom
            topMargin = top
        }
}
