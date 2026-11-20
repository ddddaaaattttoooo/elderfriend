package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Caregiver mode: reached only by long-pressing the home screen header for 3
 * seconds. From here the family can hide/show tiles, open the real Settings,
 * open Aurora or the Play Store, change the default home screen, or grant permissions.
 * Nothing here changes anything irreversible.
 */
class CaregiverActivity : Activity() {

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
                Ui.dp(this@CaregiverActivity, 16), Ui.dp(this@CaregiverActivity, 24),
                Ui.dp(this@CaregiverActivity, 16), Ui.dp(this@CaregiverActivity, 16)
            )
        }

        root.addView(header(), matchWrap())
        root.addView(Ui.appsCard(this, store), matchWrap().also { it.setMargins(0, Ui.dp(this, 16), 0, 0) })
        root.addView(Ui.builtInTilesCard(this, store), matchWrap().also { it.setMargins(0, Ui.dp(this, 14), 0, 0) })
        root.addView(actionsCard(), matchWrap().also { it.setMargins(0, Ui.dp(this, 14), 0, 0) })

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
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.rounded(this@CaregiverActivity, Color.parseColor("#FF1E2A3A"), 24)
            elevation = Ui.dp(this@CaregiverActivity, 3).toFloat()
            setPadding(
                Ui.dp(this@CaregiverActivity, 18), Ui.dp(this@CaregiverActivity, 20),
                Ui.dp(this@CaregiverActivity, 18), Ui.dp(this@CaregiverActivity, 20)
            )
        }
        card.addView(Ui.text(this, getString(R.string.caregiver), 27f, Color.WHITE, true).apply {
            gravity = Gravity.START
        })
        card.addView(Ui.note(this, getString(R.string.caregiver_intro)).also {
            it.setPadding(0, Ui.dp(this, 6), 0, 0)
        })
        return card
    }


    private fun actionsCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.sectionTitle(this, getString(R.string.caregiver_actions)))
        addButton(card, getString(R.string.restart), "#FF7A2E00") {
            val home = Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(home)
            finish()
        }
        addButton(card, getString(R.string.caregiver_settings), "#FF0B4F9C") {
            safeStart(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
        addButton(card, getString(R.string.caregiver_home), "#FF5B3A9C") {
            safeStart(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        }
        addButton(card, getString(R.string.caregiver_play_store), "#FF1E6E8C") {
            Apps.launchOrSay(this, Apps.PLAY_STORE, getString(R.string.caregiver_play_store))
        }
        if (Apps.isInstalled(this, Apps.AURORA)) {
            addButton(card, getString(R.string.caregiver_aurora), "#FF10603C") {
                Apps.launchOrSay(this, Apps.AURORA, getString(R.string.caregiver_aurora))
            }
        }
        addButton(card, getString(R.string.caregiver_fav_choose, Favourites.list(this).size), "#FF0E7490") {
            startActivity(Intent(this, AppPickerActivity::class.java).putExtra(AppPickerActivity.EXTRA_CONTACTS, true))
        }
        val sosNumber = Sos.number(this)
        addButton(
            card,
            if (sosNumber.isEmpty()) getString(R.string.caregiver_sos_set) else getString(R.string.caregiver_sos_number, sosNumber),
            "#FFB00020"
        ) { askSosNumber() }
        val missing = (listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        ) + if (sosNumber.isEmpty()) emptyList() else Sos.PERMISSIONS).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            addButton(card, "✓ " + getString(R.string.caregiver_permissions), "#FF1F8A4C") {
                Ui.info(this, getString(R.string.caregiver_permissions) + ": OK")
            }
        } else {
            addButton(card, getString(R.string.caregiver_permissions), "#FF8A4B00") {
                requestPermissions(missing.toTypedArray(), 1)
            }
        }
        // No INTERNET permission: the browser shows the latest release and downloads the APK.
        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { null }
        addButton(card, getString(R.string.caregiver_update, version.orEmpty()), "#FF334155") {
            safeStart(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/ddddaaaattttoooo/elderfriend/releases/latest")))
        }
        addButton(card, getString(R.string.close), "#FF374151") { finish() }
        return card
    }

    /** Empty number hides the SOS tile; a number asks for location + SMS. */
    private fun askSosNumber() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(Sos.number(this@CaregiverActivity))
            textSize = 28f
            hint = getString(R.string.caregiver_sos_hint)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.caregiver_sos_set)
            .setView(input)
            .setPositiveButton(R.string.yes) { _, _ ->
                Sos.setNumber(this, input.text.toString())
                val need = Sos.PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                if (Sos.number(this).isNotEmpty() && need.isNotEmpty()) requestPermissions(need.toTypedArray(), 2)
                else showScreen()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    /** Redraw so the Permissions button turns green once everything is granted. */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        showScreen()
    }

    private fun addButton(parent: LinearLayout, label: String, color: String, onClick: () -> Unit) {
        parent.addView(
            Ui.bigButton(this, label, Color.parseColor(color), 26f, 84) { onClick() },
            matchWrap().also { it.setMargins(0, Ui.dp(this, 10), 0, 0) }
        )
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Ui.info(this, getString(R.string.unknown))
        }
    }


}
