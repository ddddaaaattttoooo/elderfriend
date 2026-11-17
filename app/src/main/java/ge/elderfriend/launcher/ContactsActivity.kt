package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Contacts screen: every contact in the phone book, as huge tiles, two per row,
 * scrolling down for the rest. Tapping one asks "Call <name>?" with an
 * enormous Yes / No. Nothing here is hidden or filtered.
 *
 * Everything happens here: if contacts cannot be read, the screen asks for
 * access itself and never hands the user off to the stock contacts app.
 */
class ContactsActivity : Activity() {

    private val twoColumnWidth = 1
    private val requestCode = 1

    /** first letter of a name -> the row that holds it, for the jump bar. */
    private val letterRows = LinkedHashMap<String, LinearLayout>()
    private var listScroll: ScrollView? = null
    private var titleView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen()
    }

    private fun showScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_home)
            setPadding(Ui.dp(this@ContactsActivity, 8), Ui.dp(this@ContactsActivity, 10), Ui.dp(this@ContactsActivity, 8), Ui.dp(this@ContactsActivity, 8))
        }
        titleView = Ui.text(this, getString(R.string.contacts), 28f, Color.parseColor("#FFB9C6D2"))
        root.addView(
            titleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val people = loadContacts()
        if (people.isEmpty()) {
            val granted = checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
            root.addView(
                if (granted) {
                    Ui.text(this, getString(R.string.no_contacts), 28f, Color.parseColor("#FFB9C6D2"), true).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                } else {
                    Ui.bigButton(
                        this,
                        getString(R.string.grant_access),
                        Color.parseColor("#FF0B4F9C"),
                        30f,
                        160
                    ) {
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            requestCode
                        )
                    }
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        } else {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val list = buildList(people)
            listScroll = list
            row.addView(
                list,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            )
            val line = buildLetterLine()
            if (line != null) {
                root.addView(
                    line,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Ui.dp(this, 140)
                    )
                )
            }
            root.addView(
                row,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

        root.addView(
            Ui.bigButton(this, getString(R.string.home), Color.parseColor("#FF2A2A2A"), 30f, 96) {
                finish()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(root)
    }

    /** Rebuild in place after the user allows access; no restart, no other app. */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            showScreen()
        }
    }

    /** Two tiles per row inside a ScrollView, so every contact stays reachable. */
    private fun buildList(people: List<Pair<String, String>>): ScrollView {
        val colors = intArrayOf(
            Color.parseColor("#FF146C3A"),
            Color.parseColor("#FF0B4F9C")
        )
        val rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val margin = Ui.dp(this, 5)
        val tileHeight = Ui.dp(this, 116)

        people.forEachIndexed { index, person ->
            val tile = Ui.tile(this, person.first, colors[(index / twoColumnWidth) % colors.size], subtitle = person.second) {
                Ui.confirm(this, getString(R.string.call_question, person.first), person.second) {
                    Apps.call(this, person.second)
                }
            }
            val params = LinearLayout.LayoutParams(0, tileHeight, 1f).also {
                it.setMargins(margin, margin, margin, margin)
            }

            if (index % twoColumnWidth == 0) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                rows.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
                val letter = indexKey(person.first)
                if (letter.isNotEmpty() && !letterRows.containsKey(letter)) {
                    letterRows[letter] = row
                }
            }
            val row = rows.getChildAt(rows.childCount - 1) as LinearLayout
            row.addView(tile, params)
        }

        // An odd count leaves a gap; fill it so the last row looks intentional.
        val lastRow = rows.getChildAt(rows.childCount - 1) as? LinearLayout
        if (people.size % twoColumnWidth != 0 && lastRow != null) {
            lastRow.addView(android.view.View(this), LinearLayout.LayoutParams(0, tileHeight, 1f))
        }

        return ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_home)
            isFillViewport = true
            addView(rows)
        }
    }

    private fun indexKey(name: String): String {
        val ch = name.trim().take(1)
        if (ch.isEmpty()) return ""
        if (!ch[0].isLetterOrDigit()) return "#"
        val code = ch[0].code
        val georgian = (code in 0x10A0..0x10FF) || (code in 0x1C90..0x1CBF) || (code in 0x2D00..0x2D2F)
        return if (georgian) ch.lowercase() else ch.uppercase()
    }

    /**
     * One horizontal line of big letter cards, one per starting letter used by
     * the contacts (Latin, Cyrillic, Georgian, digit). The line scrolls
     * sideways; tapping a card jumps the contact list to that letter. Cards are
     * wide enough that the glyph is never clipped or overlapped.
     */
    private fun buildLetterLine(): View? {
        // Always show the full Latin alphabet so the index looks complete, then
        // any other initials that are actually used (Cyrillic, Georgian, digits)
        // and "#" for names that do not start with a letter or a digit.
        val alphabet = ('A'..'Z').map { it.toString() }
        val extras = letterRows.keys
            .filter { it.length == 1 && (it[0] !in 'A'..'Z') && it != "#" }
            .sorted()
        val letters = alphabet + extras + "#"
        val cols = (letters.size + 1) / 2
        val grid = GridLayout(this).apply {
            columnCount = cols
            rowCount = 2
            setPadding(0, 0, Ui.dp(this@ContactsActivity, 6), 0)
        }
        val cells = HashMap<String, TextView>()

        fun highlight(sel: String?) {
            for ((letter, cell) in cells) {
                cell.background = Ui.rounded(
                    this,
                    if (letter == sel) Color.parseColor("#FF0B4F9C") else Color.parseColor("#FF2A2A2A"),
                    12
                )
            }
        }

        for (idx in letters.indices) {
            val letter = letters[idx]
            val hasRow = letterRows.containsKey(letter)
            val cell = Ui.text(
                this,
                letter,
                28f,
                if (hasRow) Color.WHITE else Color.parseColor("#FF64727F"),
                true
            ).apply {
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                isClickable = hasRow
                background = Ui.rounded(
                    this@ContactsActivity,
                    if (hasRow) Color.parseColor("#FF2A2A2A") else Color.parseColor("#FF1A222C"),
                    12
                )
                setOnClickListener {
                    highlight(letter)
                    letterRows[letter]?.let { listScroll?.scrollTo(0, it.top) }
                    titleView?.apply {
                        text = letter
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 40f)
                        setTextColor(Color.WHITE)
                    }
                    postDelayed({
                        highlight(null)
                        titleView?.apply {
                            text = this@ContactsActivity.getString(R.string.contacts)
                            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 28f)
                            setTextColor(Color.parseColor("#FFB9C6D2"))
                        }
                    }, 1600)
                }
            }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(idx / cols),
                GridLayout.spec(idx % cols)
            )
            lp.width = Ui.dp(this, 60)
            lp.height = Ui.dp(this, 64)
            lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2))
            grid.addView(cell, lp)
            cells[letter] = cell
        }

        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(grid)
        }
    }

    /** name -> number, first number per name, alphabetical. No cap: show all. */
    private fun loadContacts(): List<Pair<String, String>> {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val found = LinkedHashMap<String, String>()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)?.trim().orEmpty()
                    val number = cursor.getString(1)?.trim().orEmpty()
                    if (name.isNotEmpty() && number.isNotEmpty() && !found.containsKey(name)) {
                        found[name] = number
                    }
                }
            }
        } catch (e: Exception) {
            // Provider unavailable: the empty list path offers the stock app.
        }
        return found.map { it.key to it.value }
    }
}
