package ge.elderfriend.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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


    /** Rounded background that visibly darkens while pressed (taps need feedback). */
    /** [color] at 14 % over white: a light card that still reads as that colour. */
    fun tint(color: Int): Int {
        fun mix(c: Int) = 255 - (255 - c) * 14 / 100
        return Color.rgb(mix(Color.red(color)), mix(Color.green(color)), mix(Color.blue(color)))
    }

    fun pressable(ctx: Context, color: Int, radiusDp: Int, pressColor: Int = 0x33000000): RippleDrawable =
        RippleDrawable(
            ColorStateList.valueOf(pressColor),
            rounded(ctx, color, radiusDp),
            rounded(ctx, Color.WHITE, radiusDp)
        )

    /** A rounded panel used by the setup and caregiver screens. */
    fun card(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(ctx, Color.parseColor("#FF18212D"), 22)
        elevation = dp(ctx, 2).toFloat()
        setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16))
    }

    /**
     * "Apps on the home screen": the chosen apps as icon + name, and a button
     * to the picker. Used by setup and caregiver mode; rebuild it on resume.
     */
    fun appsCard(activity: Activity, store: TileStore): LinearLayout {
        val ctx = activity
        val c = card(ctx)
        c.addView(sectionTitle(ctx, ctx.getString(R.string.caregiver_apps)))
        c.addView(note(ctx, ctx.getString(R.string.apps_note)).also {
            it.setPadding(0, dp(ctx, 6), 0, dp(ctx, 10))
        })
        val chosen = store.apps().filter { Apps.isInstalled(ctx, it) }
        if (chosen.isEmpty()) {
            c.addView(note(ctx, ctx.getString(R.string.no_apps_chosen)).also {
                it.setPadding(0, 0, 0, dp(ctx, 4))
            })
        }
        for (pkg in chosen) {
            c.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(ctx, 6), 0, dp(ctx, 6))
                addView(ImageView(ctx).apply { setImageDrawable(Apps.icon(ctx, pkg)) },
                    LinearLayout.LayoutParams(dp(ctx, 44), dp(ctx, 44)))
                addView(text(ctx, Apps.label(ctx, pkg), 22f, Color.WHITE).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).also {
                    it.setMargins(dp(ctx, 14), 0, 0, 0)
                })
            })
        }
        c.addView(
            bigButton(ctx, ctx.getString(R.string.choose_apps), Color.parseColor("#FF0B6E4F"), 24f, 80) {
                ctx.startActivity(android.content.Intent(ctx, AppPickerActivity::class.java))
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also { it.setMargins(0, dp(ctx, 10), 0, 0) }
        )
        return c
    }

    /** Checkboxes for the built-in tiles (call, contacts, …). */
    fun builtInTilesCard(activity: Activity, store: TileStore): LinearLayout {
        val ctx = activity
        val c = card(ctx)
        c.addView(sectionTitle(ctx, ctx.getString(R.string.caregiver_tiles)))
        c.addView(note(ctx, ctx.getString(R.string.setup_tiles_note)).also {
            it.setPadding(0, dp(ctx, 6), 0, dp(ctx, 8))
        })
        val tint = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#FF22C55E"), Color.parseColor("#FF90A4AE"))
        )
        for (id in TileStore.BUILT_IN) {
            c.addView(android.widget.CheckBox(ctx).apply {
                text = builtInLabel(ctx, id)
                isChecked = store.visible(id)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
                setTextColor(Color.WHITE)
                buttonTintList = tint
                isFocusable = false
                minHeight = dp(ctx, 62)
                setPadding(dp(ctx, 4), dp(ctx, 8), 0, dp(ctx, 8))
                setOnCheckedChangeListener { _, checked -> store.setVisible(id, checked) }
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        return c
    }

    fun builtInLabel(ctx: Context, id: String): String = ctx.getString(
        when (id) {
            "call" -> R.string.tile_call
            "contacts" -> R.string.tile_contacts
            "missed" -> R.string.tile_missed
            "recent" -> R.string.tile_recent
            "camera" -> R.string.tile_camera
            else -> R.string.tile_messages
        }
    )

    fun sectionTitle(ctx: Context, value: CharSequence): TextView =
        text(ctx, value, 23f, Color.WHITE, true).apply { gravity = Gravity.START }

    fun note(ctx: Context, value: CharSequence): TextView =
        text(ctx, value, 18f, Color.parseColor("#FFB9C6D4")).apply {
            gravity = Gravity.START
            setLineSpacing(dp(ctx, 4).toFloat(), 1f)
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
            // White card, big logo, and the name underneath: the logo is
            // recognised at a glance, the name settles any doubt.
            // Built-in tiles get a soft wash of their own colour so each one is
            // told apart by colour too; app logos keep a plain white card.
            val vector = icon is VectorDrawable
            background = pressable(ctx, if (vector) tint(bg) else Color.WHITE, 22)
            elevation = dp(ctx, 3).toFloat()
            val iconView = ImageView(ctx).apply {
                setImageDrawable(icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                if (vector) setColorFilter(bg)
            }
            val name = text(ctx, label, 24f, Color.parseColor("#FF1F2937"), true).apply {
                // Several words may wrap to 2 lines (smaller, so the icon stays the main thing);
                // a single word stays on 1 so it never splits.
                maxLines = if (' ' in label.trim()) 2 else 1
                setAutoSizeTextTypeUniformWithConfiguration(14, if (maxLines == 2) 19 else 28, 1, TypedValue.COMPLEX_UNIT_SP)
            }
            setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 6))
            addView(iconView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            name.setPadding(0, dp(ctx, 2), 0, 0)
            addView(name, LinearLayout.LayoutParams(MATCH_PARENT, dp(ctx, if (name.maxLines == 2) 44 else 38)))
        } else {
            background = pressable(ctx, bg, 18, 0x40FFFFFF)
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
        background = pressable(ctx, bg, 16, 0x40FFFFFF)
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
