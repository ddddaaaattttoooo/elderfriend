package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * The home screen.
 *
 * Header: time, date, battery, network. Long-press the header for 3 s to reach
 * caregiver mode (a 1.5 s system long-press would be too easy to hit).
 * Body: a 2-column grid of tiles. Tiles whose app is not installed are hidden
 * automatically, so the home screen never shows a dead button.
 *
 * Crash safety: an uncaught exception increments a persisted counter; two
 * crashes in a row show a plain fallback screen with a big "Continue" button.
 * A crash counter — not "the screen was left quickly" — decides this, because
 * pressing Back used to be counted as a crash (that put the real phone on the
 * fallback screen).
 */
class MainActivity : Activity() {

    private companion object {
        const val TAG = "ElderfriendHome"
        const val PREFS = "elder"
    }

    private data class TileSpec(
        val id: String,
        val label: String,
        val color: Int,
        val pkg: String? = null,
        val iconRes: Int? = null,
        val onClick: (Activity) -> Unit
    )

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var store: TileStore
    private var clock: TextView? = null
    private var dateView: TextView? = null
    private var batteryView: TextView? = null
    private var signalView: SignalBars? = null
    private var noNetworkView: TextView? = null
    private var phoneListener: PhoneStateListener? = null
    private var ready = false

    private val ticker = object : Runnable {
        override fun run() {
            refreshClock()
            ui.postDelayed(this, 15_000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = showBattery(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TileStore(this)
        installCrashRecorder()

        try {
            if (prefs().getInt("crash_streak", 0) >= 2) {
                showSafeMode()
                return
            }
            setContentView(R.layout.activity_main)
            ready = true
            bindHeader()
            bindFooter()
            buildTiles()
            if (!prefs().getBoolean("setup_done", false)) {
                startActivity(Intent(this, SetupActivity::class.java))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "home screen failed to build", t)
            ready = false
            showSafeMode()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!ready) return
        try {
            buildTiles()
            refreshClock()
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            showBattery(null)
            startSignal()
            ui.removeCallbacks(ticker)
            ui.post(ticker)
            // Survived long enough to be usable: clear any previous crash streak.
            ui.postDelayed({ prefs().edit().putInt("crash_streak", 0).apply() }, 4_000)
            askPermissions()
        } catch (t: Throwable) {
            Log.e(TAG, "home screen failed to resume", t)
            ready = false
            showSafeMode()
        }
    }

    override fun onPause() {
        super.onPause()
        // Also cancels a pending caregiver timer and the crash-streak reset.
        ui.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // not registered
        }
        phoneListener?.let { listener ->
            try {
                val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_NONE)
            } catch (e: Exception) {
                // ignore
            }
        }
        phoneListener = null
    }

    /**
     * A home screen must not close. Back used to finish this activity, and a
     * repeated Back ended up on the safe-mode screen; refresh instead.
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (ready) {
            buildTiles()
            refreshClock()
        }
    }

    private fun prefs() = getSharedPreferences(PREFS, MODE_PRIVATE)

    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val p = prefs()
                p.edit().putInt("crash_streak", p.getInt("crash_streak", 0) + 1).apply()
                Log.e(TAG, "uncaught exception recorded", error)
            } catch (e: Exception) {
                // never mask the original crash
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
            }
        }
    }

    // ---------------------------------------------------------------- header

    private fun bindHeader() {
        clock = findViewById(R.id.clock)
        dateView = findViewById(R.id.date)
        batteryView = findViewById(R.id.battery)
        signalView = findViewById(R.id.signal)
        noNetworkView = findViewById(R.id.no_network)

        val openCaregiver = Runnable {
            try {
                startActivity(Intent(this, CaregiverActivity::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "caregiver: could not start", e)
            }
        }
        val tolerance = Ui.dp(this, 30).toFloat()
        var downX = 0f
        var downY = 0f
        findViewById<View>(R.id.header).setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    ui.postDelayed(openCaregiver, 3_000)
                }
                MotionEvent.ACTION_MOVE -> {
                    // A finger always drifts a little: only a real drag cancels,
                    // otherwise "input swipe" style presses can never fire.
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (dx * dx + dy * dy > tolerance * tolerance) {
                        ui.removeCallbacks(openCaregiver)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> ui.removeCallbacks(openCaregiver)
            }
            // Must consume the gesture: returning false on ACTION_DOWN means the
            // header never sees ACTION_UP, the timer is never cancelled, and a
            // plain tap opens caregiver mode 3 s later (verified on the A7 Pro).
            true
        }
    }

    /**
     * Restart handle: a dim strip at the bottom that only fires after a 10 s
     * hold, so she cannot restart the launcher by accident but a caregiver
     * always has a way back to a fresh home screen.
     */
    private fun bindFooter() {
        val restart = Runnable { restartHome() }
        val tolerance = Ui.dp(this, 30).toFloat()
        var downX = 0f
        var downY = 0f
        findViewById<View>(R.id.footer).setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    ui.postDelayed(restart, 10_000)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (dx * dx + dy * dy > tolerance * tolerance) {
                        ui.removeCallbacks(restart)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> ui.removeCallbacks(restart)
            }
            true
        }
    }

    private fun restartHome() {
        Log.i(TAG, "restart requested")
        try {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
            finish()
        } catch (e: Exception) {
            Log.w(TAG, "restart failed", e)
        }
    }

    private fun refreshClock() {
        clock?.text = DateFormat.getTimeFormat(this).format(Date())
        val raw = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
        dateView?.text = raw.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun showBattery(intent: Intent?) {
        val info = intent
            ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return
        val level = info.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = info.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = info.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        batteryView?.text = if (level < 0) "" else
            "${level * 100 / scale}%" + if (charging) " ⚡" else ""
    }

    private fun startSignal() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "signal: READ_PHONE_STATE not granted")
            return
        }
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        @Suppress("DEPRECATION")
        val listener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                signalStrength?.let { showSignal(it.level) }
            }

            override fun onServiceStateChanged(serviceState: android.telephony.ServiceState?) {
                if (serviceState == null) return
                if (serviceState.state != android.telephony.ServiceState.STATE_IN_SERVICE) {
                    showNoNetwork()
                }
            }
        }
        phoneListener = listener
        try {
            @Suppress("DEPRECATION")
            tm.listen(
                listener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or
                    PhoneStateListener.LISTEN_SERVICE_STATE
            )
        } catch (e: Exception) {
            Log.w(TAG, "signal: listen() refused", e)
        }
        // This ROM returns null for both getSignalStrength() and
        // getServiceState() (verified on the A7 Pro), so the only reliable
        // initial reading is the SIM state: absent SIM means no network.
        var shown = false
        try {
            val strength = tm.signalStrength
            Log.i(TAG, "signal: initial level=${strength?.level}")
            if (strength != null) {
                showSignal(strength.level)
                shown = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "signal: getSignalStrength failed", e)
        }
        if (!shown) {
            try {
                val sim = tm.simState
                Log.i(TAG, "signal: sim state=$sim (no signal reading)")
                if (sim == TelephonyManager.SIM_STATE_ABSENT ||
                    sim == TelephonyManager.SIM_STATE_NOT_READY ||
                    sim == TelephonyManager.SIM_STATE_PIN_REQUIRED
                ) {
                    showNoNetwork()
                }
            } catch (e: Exception) {
                Log.w(TAG, "signal: getSimState failed", e)
            }
        }
    }

    private fun showSignal(level: Int) {
        val view = signalView ?: return
        if (level <= 0) {
            showNoNetwork()
            return
        }
        noNetworkView?.visibility = View.GONE
        view.setLevel(level)
    }

    private fun showNoNetwork() {
        signalView?.setLevel(0)
        noNetworkView?.visibility = View.VISIBLE
    }

    // ----------------------------------------------------------------- tiles

    private fun badge(count: Int): TextView = TextView(this).apply {
        text = if (count > 9) "9+" else count.toString()
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        isClickable = false
        elevation = Ui.dp(this@MainActivity, 8).toFloat()
        background = Ui.rounded(this@MainActivity, Color.parseColor("#FFD32F2F"), 23)
    }

    private fun buildTiles() {
        val grid = findViewById<GridLayout>(R.id.tiles)
        grid.removeAllViews()
        val margin = Ui.dp(this, 6)
        for (spec in tileSpecs()) {
            if (!store.visible(spec.id)) continue
            // Never show a dead button for an app that is not installed.
            if (spec.pkg != null && !Apps.isInstalled(this, spec.pkg)) continue
            val icon = if (spec.pkg != null) {
                Apps.icon(this, spec.pkg)
            } else {
                spec.iconRes?.let { id ->
                    try {
                        getDrawable(id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            val view = Ui.tile(this, spec.label, spec.color, icon) { spec.onClick(this) }
            val missed = if (spec.id == "missed") {
                CallLogs.missedSince(this, prefs().getLong("missed_seen", 0L))
            } else {
                0
            }
            val child: View = if (missed > 0) {
                FrameLayout(this).apply {
                    addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    val size = Ui.dp(this@MainActivity, 46)
                    addView(
                        badge(missed),
                        FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).also {
                            it.setMargins(0, Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 8), 0)
                        }
                    )
                }
            } else {
                view
            }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            )
            lp.width = 0
            lp.height = 0
            lp.setMargins(margin, margin, margin, margin)
            grid.addView(child, lp)
        }
        grid.requestLayout()
    }

    /** Order matters: left to right, top to bottom, two per row. */
    private fun tileSpecs(): List<TileSpec> = listOf(
        TileSpec("call", getString(R.string.tile_call), Color.parseColor("#FF146C3A"),
            iconRes = R.drawable.ic_tile_call) { a ->
            a.startActivity(Intent(Intent.ACTION_DIAL))
        },
        TileSpec("contacts", getString(R.string.tile_contacts), Color.parseColor("#FF0B4F9C"),
            iconRes = R.drawable.ic_tile_contacts) { a ->
            a.startActivity(Intent(a, ContactsActivity::class.java))
        },
        TileSpec("missed", getString(R.string.tile_missed), Color.parseColor("#FFA32B26"),
            iconRes = R.drawable.ic_tile_missed) { a ->
            a.startActivity(
                Intent(a, CallsActivity::class.java).putExtra(CallsActivity.EXTRA_MODE, CallsActivity.MODE_MISSED)
            )
        },
        TileSpec("recent", getString(R.string.tile_recent), Color.parseColor("#FF1F6E5A"),
            iconRes = R.drawable.ic_tile_recent) { a ->
            a.startActivity(
                Intent(a, CallsActivity::class.java).putExtra(CallsActivity.EXTRA_MODE, CallsActivity.MODE_RECENT)
            )
        },
        TileSpec("camera", getString(R.string.tile_camera), Color.parseColor("#FF8A4B00"),
            iconRes = R.drawable.ic_tile_camera) { a ->
            a.startActivity(Intent(a, CameraActivity::class.java))
        },
        TileSpec("messages", getString(R.string.tile_messages), Color.parseColor("#FF5B3A9C"),
            iconRes = R.drawable.ic_tile_messages) { a ->
            Apps.openSms(a)
        },
        TileSpec("facebook", getString(R.string.tile_facebook), Color.parseColor("#FF2C3E77"), Apps.FACEBOOK) { a ->
            Apps.launchOrSay(a, Apps.FACEBOOK, a.getString(R.string.tile_facebook))
        },
        TileSpec("messenger", getString(R.string.tile_messenger), Color.parseColor("#FF1B4EA8"), Apps.MESSENGER) { a ->
            Apps.launchOrSay(a, Apps.MESSENGER, a.getString(R.string.tile_messenger))
        },
        TileSpec("whatsapp", getString(R.string.tile_whatsapp), Color.parseColor("#FF10603C"), Apps.WHATSAPP) { a ->
            Apps.launchOrSay(a, Apps.WHATSAPP, a.getString(R.string.tile_whatsapp))
        },
        TileSpec("viber", getString(R.string.tile_viber), Color.parseColor("#FF4A2A80"), Apps.VIBER) { a ->
            Apps.launchOrSay(a, Apps.VIBER, a.getString(R.string.tile_viber))
        }
    )

    // ------------------------------------------------- flight-safety systems

    /**
     * Ask once per install, never on every return to the home screen: a denial
     * loop in front of an 87-year-old is worse than degraded behaviour. The
     * caregiver screen can request again at any time.
     */
    private fun askPermissions() {
        val p = prefs()
        if (p.getBoolean("perm_asked", false)) return
        val wanted = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )
        val missing = wanted.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        p.edit().putBoolean("perm_asked", true).apply()
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)
    }

    private fun showSafeMode() {
        ready = false
        val ctx = this
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(ctx, 20), Ui.dp(ctx, 30), Ui.dp(ctx, 20), Ui.dp(ctx, 20))
        }
        fun params(topMarginDp: Int) = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.setMargins(0, Ui.dp(ctx, topMarginDp), 0, 0) }

        box.addView(
            Ui.text(this, getString(R.string.safe_mode), 28f, Color.WHITE, true),
            params(0)
        )
        box.addView(
            Ui.bigButton(this, getString(R.string.retry), Color.parseColor("#FF146C3A"), 30f, 110) {
                prefs().edit().putInt("crash_streak", 0).apply()
                recreate()
            },
            params(24)
        )
        box.addView(
            Ui.bigButton(this, getString(R.string.caregiver_settings), Color.parseColor("#FF0B4F9C"), 26f, 96) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                } catch (e: Exception) {
                    // nothing else we can do here
                }
            },
            params(12)
        )
        box.addView(
            Ui.bigButton(this, getString(R.string.caregiver), Color.parseColor("#FF444444"), 26f, 96) {
                startActivity(Intent(this, CaregiverActivity::class.java))
            },
            params(12)
        )
        setContentView(box)
    }
}
