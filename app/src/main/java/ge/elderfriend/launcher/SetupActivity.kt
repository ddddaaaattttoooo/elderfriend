package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * First-run setup for the family member who prepares the phone: permissions,
 * which tiles to show, and making Elderfriend the home app. Everything here is
 * optional and can be repeated later from Caregiver mode.
 */
class SetupActivity : Activity() {

    private val requestCode = 3
    private lateinit var store: TileStore
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TileStore(this)
        showScreen()
    }

    private fun showScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(
                Ui.dp(this@SetupActivity, 16), Ui.dp(this@SetupActivity, 20),
                Ui.dp(this@SetupActivity, 16), Ui.dp(this@SetupActivity, 16)
            )
        }
        root.addView(
            Ui.text(this, getString(R.string.setup_title), 30f, Color.WHITE, true).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            Ui.text(this, getString(R.string.setup_intro), 22f, Color.parseColor("#FFBBBBBB")).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 6)) }
        )

        addButton(getString(R.string.caregiver_permissions), "#FF8A4B00") { askPermissions() }

        root.addView(
            Ui.text(this, getString(R.string.caregiver_tiles), 20f, Color.parseColor("#FFBBBBBB")).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 18), 0, Ui.dp(this, 4)) }
        )
        for (id in TileStore.ALL) {
            val missing = packageFor(id)?.let { !Apps.isInstalled(this, it) } ?: false
            val check = CheckBox(this).apply {
                text = if (missing) labelFor(id) + " (—)" else labelFor(id)
                isChecked = store.visible(id)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                setTextColor(Color.WHITE)
                minHeight = Ui.dp(this@SetupActivity, 64)
                isFocusable = false
                setPadding(0, Ui.dp(this@SetupActivity, 8), 0, Ui.dp(this@SetupActivity, 8))
                setOnCheckedChangeListener { _, checked -> store.setVisible(id, checked) }
            }
            root.addView(
                check,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        addButton(getString(R.string.caregiver_home), "#FF5B3A9C") {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                Ui.info(this, getString(R.string.unknown))
            }
        }
        addButton(getString(R.string.setup_done), "#FF146C3A") { finishSetup() }
        addButton(getString(R.string.close), "#FF444444") { finishSetup() }

        root.addView(
            Ui.text(this, getString(R.string.made_by), 18f, Color.parseColor("#FF7A8794")).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 22), 0, Ui.dp(this, 8)) }
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(root)
        }
        setContentView(scroll)
        scroll.post { scroll.scrollTo(0, 0) }
    }

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

    private fun addButton(label: String, color: String, onClick: () -> Unit) {
        root.addView(
            Ui.bigButton(this, label, Color.parseColor(color), 26f, 88) { onClick() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 10), 0, 0) }
        )
    }

    private fun labelFor(id: String): String = when (id) {
        "call" -> getString(R.string.tile_call)
        "messages" -> getString(R.string.tile_messages)
        "contacts" -> getString(R.string.tile_contacts)
        "missed" -> getString(R.string.tile_missed)
        "recent" -> getString(R.string.tile_recent)
        "camera" -> getString(R.string.tile_camera)
        "whatsapp" -> getString(R.string.tile_whatsapp)
        "viber" -> getString(R.string.tile_viber)
        "messenger" -> getString(R.string.tile_messenger)
        "facebook" -> getString(R.string.tile_facebook)
        else -> id
    }

    private fun packageFor(id: String): String? = when (id) {
        "whatsapp" -> Apps.WHATSAPP
        "viber" -> Apps.VIBER
        "messenger" -> Apps.MESSENGER
        "facebook" -> Apps.FACEBOOK
        else -> null
    }
}
