package ge.elderfriend.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shared UI primitives. Everything here is deliberately big and high contrast,
 * with no gestures beyond a plain tap (the one exception is the caregiver
 * long-press, which is documented). Home tiles show the app's own logo so the
 * button is recognisable at a glance.
 */
object Ui {

    fun dp(ctx: Context, value: Int): Int =
        (value * ctx.resources.displayMetrics.density + 0.5f).toInt()

    fun rounded(ctx: Context, color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(ctx, radiusDp).toFloat()
        }

    fun text(
        ctx: Context,
        value: CharSequence,
        sizeSp: Float,
        color: Int = Color.WHITE,
        bold: Boolean = false
    ): TextView = TextView(ctx).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    /**
     * A tile. With an icon: white card + big logo (used on the home screen).
     * Without an icon: the old coloured, word-based tile (used for contacts).
     * The whole tile is the target; the label is in contentDescription too.
     */
    fun tile(
        ctx: Context,
        label: String,
        bg: Int,
        icon: Drawable? = null,
        subtitle: String? = null,
        onClick: () -> Unit
    ): View = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = false
        contentDescription = if (subtitle.isNullOrEmpty()) label else "$label, $subtitle"
        setOnClickListener { onClick() }
        if (icon != null) {
            background = rounded(ctx, Color.WHITE, 22)
            elevation = dp(ctx, 3).toFloat()
            setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 18))
            addView(
                ImageView(ctx).apply {
                    setImageDrawable(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    if (icon is VectorDrawable) setColorFilter(bg)
                },
                LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
        } else {
            background = rounded(ctx, bg, 18)
            setPadding(dp(ctx, 10), dp(ctx, 10), dp(ctx, 10), dp(ctx, 10))
            addView(
                text(ctx, label, 30f, Color.WHITE, true).apply {
                    // Shrink long names instead of splitting words ("Alekcan / dra").
                    setAutoSizeTextTypeUniformWithConfiguration(
                        12, 36, 1, TypedValue.COMPLEX_UNIT_SP
                    )
                    maxLines = 1
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            )
            if (!subtitle.isNullOrEmpty()) {
                addView(
                    text(ctx, subtitle, 20f, Color.parseColor("#E6FFFFFF"), false).apply {
                        setAutoSizeTextTypeUniformWithConfiguration(
                            12, 24, 1, TypedValue.COMPLEX_UNIT_SP
                        )
                        maxLines = 1
                    },
                    LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                )
            }
        }
    }

    fun bigButton(
        ctx: Context,
        label: CharSequence,
        bg: Int,
        sizeSp: Float = 30f,
        heightDp: Int = 96,
        fg: Int = Color.WHITE,
        onClick: (() -> Unit)? = null
    ): Button = Button(ctx).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(fg)
        background = rounded(ctx, bg, 16)
        stateListAnimator = null
        isFocusable = false
        minHeight = dp(ctx, heightDp)
        minimumHeight = dp(ctx, heightDp)
        setPadding(dp(ctx, 10), dp(ctx, 10), dp(ctx, 10), dp(ctx, 10))
        if (onClick != null) setOnClickListener { onClick() }
    }

    private fun dialogBox(ctx: Context): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(ctx, 20), dp(ctx, 26), dp(ctx, 20), dp(ctx, 20))
            gravity = Gravity.CENTER_HORIZONTAL
        }

    private fun buttonRow(ctx: Context, yes: Button, no: Button): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(yes, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).also {
                it.setMargins(dp(ctx, 6), dp(ctx, 18), dp(ctx, 6), 0)
            })
            addView(no, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).also {
                it.setMargins(dp(ctx, 6), dp(ctx, 18), dp(ctx, 6), 0)
            })
        }

    /** One option in a [choose] sheet. */
    fun confirm(activity: Activity, question: String, subtitle: String? = null, onYes: () -> Unit) {
        val ctx = activity
        val box = dialogBox(ctx)
        box.addView(
            text(ctx, question, 28f, Color.WHITE, true).apply {
                setAutoSizeTextTypeUniformWithConfiguration(
                    16, 32, 1, TypedValue.COMPLEX_UNIT_SP
                )
                maxLines = 2
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        if (!subtitle.isNullOrEmpty()) {
            box.addView(
                text(ctx, subtitle, 24f, Color.parseColor("#E6FFFFFF"), false).apply {
                    setAutoSizeTextTypeUniformWithConfiguration(
                        14, 26, 1, TypedValue.COMPLEX_UNIT_SP
                    )
                    maxLines = 1
                    setPadding(0, 0, 0, dp(ctx, 6))
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            )
        }
        val yes = bigButton(ctx, ctx.getString(R.string.yes), Color.parseColor("#FF146C3A"), 34f, 110)
        val no = bigButton(ctx, ctx.getString(R.string.no), Color.parseColor("#FF505050"), 34f, 110)
        box.addView(buttonRow(ctx, yes, no))
        val dialog = AlertDialog.Builder(ctx, android.R.style.Theme_Material_Dialog_Alert)
            .setView(box)
            .create()
        yes.setOnClickListener {
            dialog.dismiss()
            onYes()
        }
        no.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /** Big "this needs a person" message with one big dismissal button. */
    fun info(activity: Activity, message: String) {
        val ctx = activity
        val box = dialogBox(ctx)
        box.addView(
            text(ctx, message, 30f, Color.WHITE, true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        val ok = bigButton(ctx, ctx.getString(R.string.close), Color.parseColor("#FF505050"), 30f, 96)
        box.addView(
            ok,
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.setMargins(dp(ctx, 6), dp(ctx, 18), dp(ctx, 6), 0)
            }
        )
        val dialog = AlertDialog.Builder(ctx, android.R.style.Theme_Material_Dialog_Alert)
            .setView(box)
            .create()
        ok.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
