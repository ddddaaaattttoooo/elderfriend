package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One screen for the two things she actually needs: the calls she missed and
 * the people who call her most recently. Opened from two home tiles; the mode
 * only changes the query and the title.
 */
class CallsActivity : Activity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_RECENT = "recent"
        const val MODE_MISSED = "missed"
    }

    private val requestCode = 2

    private data class Entry(val label: String, val number: String, val time: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen()
    }

    private fun missed(): Boolean = intent.getStringExtra(EXTRA_MODE) == MODE_MISSED

    private fun showScreen() {
        if (missed()) {
            getSharedPreferences("elder", MODE_PRIVATE).edit()
                .putLong("missed_seen", System.currentTimeMillis()).apply()
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_home)
            setPadding(Ui.dp(this@CallsActivity, 8), Ui.dp(this@CallsActivity, 10), Ui.dp(this@CallsActivity, 8), Ui.dp(this@CallsActivity, 8))
        }
        root.addView(
            Ui.text(
                this,
                getString(if (missed()) R.string.missed_title else R.string.recent_title),
                26f,
                Color.parseColor("#FFB9C6D2")
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val entries = loadCalls(if (missed()) CallLog.Calls.MISSED_TYPE else null)
        if (entries.isEmpty()) {
            val granted = checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED
            root.addView(
                if (granted) {
                    Ui.text(
                        this,
                        getString(if (missed()) R.string.no_missed else R.string.no_recent),
                        26f,
                        Color.parseColor("#FFB9C6D2"),
                        true
                    ).apply { gravity = android.view.Gravity.CENTER }
                } else {
                    Ui.bigButton(
                        this,
                        getString(R.string.grant_access),
                        Color.parseColor("#FF0B4F9C"),
                        30f,
                        160
                    ) {
                        requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), requestCode)
                    }
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        } else {
            val scroll = ScrollView(this)
            val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val margin = Ui.dp(this, 5)
            val columns = 1
            val tileHeight = Ui.dp(this, 116)
            val colors = if (missed()) {
                intArrayOf(Color.parseColor("#FFA32B26"), Color.parseColor("#FF7F1D1A"))
            } else {
                intArrayOf(Color.parseColor("#FF146C3A"), Color.parseColor("#FF0B4F9C"))
            }
            entries.forEachIndexed { index, entry ->
                val tile = Ui.tile(
                    this,
                    entry.label,
                    colors[(index / columns) % colors.size],
                    subtitle = if (missed()) {
                        getString(R.string.missed_mark) + " · " + entry.time
                    } else {
                        entry.time
                    }
                ) {
                    Ui.confirm(this, getString(R.string.call_question, entry.label), entry.number) {
                        Apps.call(this, entry.number)
                    }
                }
                val params = LinearLayout.LayoutParams(0, tileHeight, 1f).also {
                    it.setMargins(margin, margin, margin, margin)
                }
                if (index % columns == 0) {
                    rows.addView(
                        LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL },
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )
                }
                (rows.getChildAt(rows.childCount - 1) as LinearLayout).addView(tile, params)
            }
            scroll.addView(
                rows,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            root.addView(
                scroll,
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) showScreen()
    }

    /** Newest first, at most 60 rows; names resolved once per number. */
    private fun loadCalls(onlyType: Int?): List<Entry> {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) return emptyList()
        val out = mutableListOf<Entry>()
        val names = HashMap<String, String?>()
        val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE
                ),
                if (onlyType != null) "${CallLog.Calls.TYPE} = ?" else null,
                if (onlyType != null) arrayOf(onlyType.toString()) else null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                while (c.moveToNext() && out.size < 60) {
                    val number = c.getString(numIdx) ?: continue
                    if (number.isBlank()) continue
                    var label = c.getString(nameIdx)
                    if (label.isNullOrBlank()) {
                        if (!names.containsKey(number)) {
                            names[number] = lookupName(number)
                        }
                        label = names[number]
                    }
                    out.add(
                        Entry(
                            label?.takeIf { it.isNotBlank() } ?: number,
                            number,
                            fmt.format(Date(c.getLong(dateIdx)))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ElderfriendCalls", "loadCalls failed", e)
        }
        return out
    }

    private fun lookupName(number: String): String? = try {
        contentResolver.query(
            android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(number)
            ),
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }
}
