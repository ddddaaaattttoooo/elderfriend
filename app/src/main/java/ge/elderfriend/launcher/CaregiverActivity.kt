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
 * Caregiver mode: reached only by long-pressing the home screen header for 3
 * seconds. From here the family can hide/show tiles, open the real Settings,
 * open Aurora, change the default home screen, or grant permissions.
 * Nothing here changes anything irreversible.
 */
class CaregiverActivity : Activity() {

    private lateinit var store: TileStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TileStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(Ui.dp(this@CaregiverActivity, 16), Ui.dp(this@CaregiverActivity, 20), Ui.dp(this@CaregiverActivity, 16), Ui.dp(this@CaregiverActivity, 16))
        }

        root.addView(
            Ui.text(this, getString(R.string.caregiver), 30f, Color.WHITE, true).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            Ui.text(this, getString(R.string.caregiver_tiles), 20f, Color.parseColor("#FFBBBBBB")).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 20), 0, Ui.dp(this, 4)) }
        )

        for (id in TileStore.ALL) {
            val missing = packageFor(id)?.let { !Apps.isInstalled(this, it) } ?: false
            val check = CheckBox(this).apply {
                text = if (missing) labelFor(id) + " (—)" else labelFor(id)
                isChecked = store.visible(id)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                setTextColor(Color.WHITE)
                minHeight = Ui.dp(this@CaregiverActivity, 64)
                isFocusable = false
                setPadding(0, Ui.dp(this@CaregiverActivity, 8), 0, Ui.dp(this@CaregiverActivity, 8))
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

        addButton(root, getString(R.string.restart), "#FF7A2E00") {
            val home = Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(home)
            finish()
        }
        addButton(root, getString(R.string.caregiver_settings), "#FF0B4F9C") {
            safeStart(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
        addButton(root, getString(R.string.caregiver_home), "#FF5B3A9C") {
            safeStart(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        }
        addButton(root, getString(R.string.caregiver_aurora), "#FF10603C") {
            Apps.launchOrSay(this, Apps.AURORA, getString(R.string.caregiver_aurora))
        }
        addButton(root, getString(R.string.caregiver_permissions), "#FF8A4B00") {
            val wanted = listOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE
            ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (wanted.isEmpty()) {
                Ui.info(this, getString(R.string.caregiver_permissions) + ": OK")
            } else {
                requestPermissions(wanted.toTypedArray(), 1)
            }
        }
        addButton(root, getString(R.string.close), "#FF444444") { finish() }

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

    private fun addButton(parent: LinearLayout, label: String, color: String, onClick: () -> Unit) {
        parent.addView(
            Ui.bigButton(this, label, Color.parseColor(color), 26f, 88) { onClick() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 10), 0, 0) }
        )
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Ui.info(this, getString(R.string.unknown))
        }
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

    /** App-backed tiles are hidden automatically while the app is missing. */
    private fun packageFor(id: String): String? = when (id) {
        "whatsapp" -> Apps.WHATSAPP
        "viber" -> Apps.VIBER
        "messenger" -> Apps.MESSENGER
        "facebook" -> Apps.FACEBOOK
        else -> null
    }
}
