package ge.elderfriend.launcher

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Caregiver-only: pick which installed apps get a tile on the home screen.
 * Chosen apps are listed first, in home-screen order; ticking an app adds it
 * to the end, unticking removes it. Saved on every change, so Back is safe.
 */
class AppPickerActivity : Activity() {

    private class Row(val pkg: String, val label: String, val view: View, val check: CheckBox)

    private lateinit var store: TileStore
    private val chosen = mutableListOf<String>()
    private val rows = mutableListOf<Row>()
    private lateinit var countView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TileStore(this)
        chosen += store.apps()

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_home)
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }
        page.addView(header(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        page.addView(search(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
            it.setMargins(0, dp(14), 0, dp(8))
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val installed = Apps.launchable(this)
        // Chosen first (in their order), then everything else alphabetically.
        val ordered = chosen.filter { it in installed } + installed.filter { it !in chosen }
        for (pkg in ordered) {
            val row = row(pkg)
            rows += row
            list.addView(row.view, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.setMargins(0, dp(4), 0, dp(4))
            })
        }
        page.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        page.addView(
            Ui.bigButton(this, getString(R.string.setup_done), Color.parseColor("#FF15803D"), 26f, 84) {
                finish()
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also { it.setMargins(0, dp(12), 0, 0) }
        )
        setContentView(page)
        updateCount()
    }

    private fun dp(v: Int) = Ui.dp(this, v)

    private fun header(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.rounded(this@AppPickerActivity, Color.parseColor("#FF1E2A3A"), 24)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        card.addView(Ui.text(this, getString(R.string.choose_apps), 26f, Color.WHITE, true).apply {
            gravity = Gravity.START
        })
        card.addView(Ui.note(this, getString(R.string.apps_note)).also {
            it.setPadding(0, dp(6), 0, 0)
        })
        countView = Ui.text(this, "", 18f, Color.parseColor("#FF86EFAC"), true).apply {
            gravity = Gravity.START
            setPadding(0, dp(8), 0, 0)
        }
        card.addView(countView)
        return card
    }

    private fun search(): EditText = EditText(this).apply {
        hint = getString(R.string.search)
        setHintTextColor(Color.parseColor("#FF7A8794"))
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        isSingleLine = true
        background = Ui.rounded(this@AppPickerActivity, Color.parseColor("#FF18212D"), 16)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        for (r in rows) {
            r.view.visibility = if (q.isEmpty() || r.label.lowercase().contains(q)) View.VISIBLE else View.GONE
        }
    }

    private fun row(pkg: String): Row {
        val label = Apps.label(this, pkg)
        val check = CheckBox(this).apply {
            isChecked = pkg in chosen
            isClickable = false
            isFocusable = false
            buttonTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.parseColor("#FF22C55E"), Color.parseColor("#FF90A4AE"))
            )
            scaleX = 1.4f
            scaleY = 1.4f
        }
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.pressable(this@AppPickerActivity, Color.parseColor("#FF18212D"), 18)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            minimumHeight = dp(76)
            contentDescription = label
            addView(ImageView(this@AppPickerActivity).apply {
                setImageDrawable(Apps.icon(this@AppPickerActivity, pkg))
            }, LinearLayout.LayoutParams(dp(52), dp(52)))
            addView(Ui.text(this@AppPickerActivity, label, 22f, Color.WHITE).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                maxLines = 2
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).also { it.setMargins(dp(16), 0, dp(8), 0) })
            addView(check, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
                it.setMargins(0, 0, dp(6), 0)
            })
            setOnClickListener {
                check.isChecked = !check.isChecked
                if (check.isChecked) chosen += pkg else chosen -= pkg
                store.setApps(chosen)
                updateCount()
            }
        }
        return Row(pkg, label, view, check)
    }

    private fun updateCount() {
        countView.text = getString(R.string.apps_chosen, chosen.size)
    }
}
