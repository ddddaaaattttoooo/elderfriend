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
 * Caregiver-only: pick which installed apps get a tile on the home screen,
 * or (with [EXTRA_CONTACTS]) which people go on the Favourites screen.
 * Chosen apps are listed first, in home-screen order; ticking an app adds it
 * to the end, unticking removes it. Saved on every change, so Back is safe.
 */
class AppPickerActivity : Activity() {

    private class Row(val pkg: String, val label: String, val view: View, val check: CheckBox)
    private class Item(val key: String, val label: String, val icon: android.graphics.drawable.Drawable?, val sub: String? = null)

    private val contacts by lazy { intent.getBooleanExtra(EXTRA_CONTACTS, false) }
    private val people by lazy { Favourites.all(this).associateBy { it.number } }

    private lateinit var store: TileStore
    private val chosen = mutableListOf<String>()
    private val rows = mutableListOf<Row>()
    private lateinit var countView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TileStore(this)
        chosen += if (contacts) Favourites.list(this).map { it.number } else store.apps()

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
        val items = if (contacts) {
            people.values.map { Item(it.number, it.name, Favourites.photo(this, it) ?: getDrawable(R.drawable.ic_tile_contacts), it.number) }
        } else {
            Apps.launchable(this).map { Item(it, Apps.label(this, it), Apps.icon(this, it)) }
        }
        val byKey = items.associateBy { it.key }
        // Chosen first (in their order), then everything else alphabetically.
        val ordered = chosen.mapNotNull { byKey[it] } + items.filter { it.key !in chosen }
        for (item in ordered) {
            val row = row(item)
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
        card.addView(Ui.text(this, getString(if (contacts) R.string.choose_contacts else R.string.choose_apps), 26f, Color.WHITE, true).apply {
            gravity = Gravity.START
        })
        card.addView(Ui.note(this, getString(if (contacts) R.string.contacts_note else R.string.apps_note)).also {
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

    private fun row(item: Item): Row {
        val pkg = item.key
        val label = item.label
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
                setImageDrawable(item.icon)
            }, LinearLayout.LayoutParams(dp(52), dp(52)))
            addView(LinearLayout(this@AppPickerActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(Ui.text(this@AppPickerActivity, label, 22f, Color.WHITE).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    maxLines = 2
                })
                // Contacts: the number tells apart two people with the same name.
                if (item.sub != null) addView(Ui.text(this@AppPickerActivity, item.sub, 18f, Color.parseColor("#FF9FB0C0")).apply {
                    gravity = Gravity.START
                })
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).also { it.setMargins(dp(16), 0, dp(8), 0) })
            addView(check, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
                it.setMargins(0, 0, dp(6), 0)
            })
            setOnClickListener {
                check.isChecked = !check.isChecked
                if (check.isChecked) chosen += pkg else chosen -= pkg
                if (contacts) Favourites.set(this@AppPickerActivity, chosen.mapNotNull { people[it] })
                else store.setApps(chosen)
                updateCount()
            }
        }
        return Row(pkg, label, view, check)
    }

    private fun updateCount() {
        countView.text = getString(R.string.apps_chosen, chosen.size)
    }

    companion object {
        const val EXTRA_CONTACTS = "contacts"
    }
}
