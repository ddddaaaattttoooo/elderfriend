package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * First-run setup for the person who prepares the phone: permissions, which
 * tiles to show, and making Elderfriend the home app. Every step is optional
 * and can be repeated later from caregiver mode.
 */
class SetupActivity : Activity() {

    private val requestCode = 3
    private lateinit var store: TileStore
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TileStore(this)
    }

    /** Rebuilt on every resume so the apps card shows what the picker saved. */
    override fun onResume() {
        super.onResume()
        showScreen()
    }

    private fun showScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                Ui.dp(this@SetupActivity, 16), Ui.dp(this@SetupActivity, 24),
                Ui.dp(this@SetupActivity, 16), Ui.dp(this@SetupActivity, 16)
            )
        }

        root.addView(header(), matchWrap())
        root.addView(permissionsCard(), matchWrap().also { it.setMargins(0, Ui.dp(this, 16), 0, 0) })
        root.addView(Ui.appsCard(this, store), matchWrap().also { it.setMargins(0, Ui.dp(this, 14), 0, 0) })
        root.addView(Ui.builtInTilesCard(this, store), matchWrap().also { it.setMargins(0, Ui.dp(this, 14), 0, 0) })
        root.addView(homeCard(), matchWrap().also { it.setMargins(0, Ui.dp(this, 14), 0, 0) })

        addButton(getString(R.string.setup_done), "#FF15803D", 92f)
        addButton(getString(R.string.close), "#FF374151", 80f)

        root.addView(
            Ui.text(this, getString(R.string.made_by), 18f, Color.parseColor("#FF7A8794")).apply {
                gravity = Gravity.CENTER
            },
            matchWrap().also { it.setMargins(0, Ui.dp(this, 24), 0, Ui.dp(this, 8)) }
        )

        val scroll = ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_home)
            addView(root)
        }
        setContentView(scroll)
        scroll.post { scroll.scrollTo(0, 0) }
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

    private fun header(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.rounded(this@SetupActivity, Color.parseColor("#FF1E2A3A"), 24)
            elevation = Ui.dp(this@SetupActivity, 3).toFloat()
            setPadding(
                Ui.dp(this@SetupActivity, 18), Ui.dp(this@SetupActivity, 18),
                Ui.dp(this@SetupActivity, 18), Ui.dp(this@SetupActivity, 18)
            )
        }
        row.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72)).also {
            it.setMargins(0, 0, Ui.dp(this, 14), 0)
        })
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(Ui.text(this, getString(R.string.setup_title), 25f, Color.WHITE, true).apply {
            gravity = Gravity.START
        })
        column.addView(Ui.note(this, getString(R.string.setup_intro)).also {
            it.setPadding(0, Ui.dp(this, 6), 0, 0)
        })
        row.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun permissionsCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.sectionTitle(this, getString(R.string.caregiver_permissions)))
        card.addView(Ui.note(this, getString(R.string.setup_permissions_note)).also {
            it.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12))
        })
        val ok = permissionsOk()
        card.addView(
            Ui.bigButton(
                this,
                if (ok) getString(R.string.caregiver_permissions) + "  ✓" else getString(R.string.grant_access),
                Color.parseColor(if (ok) "#FF15803D" else "#FFB45309"),
                24f,
                80
            ) { askPermissions() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return card
    }


    private fun homeCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.sectionTitle(this, getString(R.string.caregiver_home)))
        card.addView(Ui.note(this, getString(R.string.setup_home_note)).also {
            it.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12))
        })
        card.addView(
            Ui.bigButton(this, getString(R.string.caregiver_home), Color.parseColor("#FF5B3A9C"), 24f, 80) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
                } catch (e: Exception) {
                    Ui.info(this, getString(R.string.unknown))
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return card
    }

    private fun permissionsOk(): Boolean = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    ).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun askPermissions() {
        val wanted = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isEmpty()) {
            Ui.info(this, getString(R.string.caregiver_permissions) + ": OK")
        } else {
            requestPermissions(wanted.toTypedArray(), requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) showScreen()
    }

    private fun finishSetup() {
        getSharedPreferences("elder", MODE_PRIVATE).edit()
            .putBoolean("setup_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun addButton(label: String, color: String, heightDp: Float) {
        root.addView(
            Ui.bigButton(this, label, Color.parseColor(color), 27f, heightDp.toInt()) { finishSetup() },
            matchWrap().also { it.setMargins(0, Ui.dp(this, 14), 0, 0) }
        )
    }


}
