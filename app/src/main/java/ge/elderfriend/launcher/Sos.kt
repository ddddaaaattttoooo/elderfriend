package ge.elderfriend.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager

/**
 * SOS: text the caregiver a map link (when location and SMS are allowed),
 * then call them. Every failure still ends in the call.
 */
object Sos {

    /** Asked for only once an SOS number is set. */
    val PERMISSIONS = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS)

    private const val KEY = "sos_number"
    private const val FRESH_MS = 10 * 60 * 1000L
    private const val WAIT_MS = 15_000L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("elder", Context.MODE_PRIVATE)

    fun number(ctx: Context): String = prefs(ctx).getString(KEY, "").orEmpty()

    fun setNumber(ctx: Context, number: String) {
        prefs(ctx).edit().putString(KEY, number.trim()).apply()
    }

    private fun granted(ctx: Context, p: String) =
        ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    fun send(activity: Activity) {
        val number = number(activity)
        if (number.isEmpty()) return
        val call = { Apps.call(activity, number) }
        if (!granted(activity, Manifest.permission.SEND_SMS)) return call()
        android.widget.Toast.makeText(activity, R.string.sos_sending, android.widget.Toast.LENGTH_LONG).show()
        locate(activity) { loc ->
            val where = loc?.let { "https://maps.google.com/?q=%.6f,%.6f".format(java.util.Locale.US, it.latitude, it.longitude) }
                ?: activity.getString(R.string.sos_no_location)
            try {
                // Georgian/Russian text is UCS-2: 70 chars per SMS, so always split.
                @Suppress("DEPRECATION")
                val sms = SmsManager.getDefault()
                sms.sendMultipartTextMessage(number, null, sms.divideMessage(activity.getString(R.string.sos_sms, where)), null, null)
            } catch (e: Exception) {
                // No SIM or SMS blocked: the call below still goes through.
            }
            call()
        }
    }

    /** Recent last-known fix right away, else wait up to 15 s for a fresh one. */
    @SuppressLint("MissingPermission")
    private fun locate(ctx: Context, done: (Location?) -> Unit) {
        if (!granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)) return done(null)
        val lm = ctx.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < FRESH_MS) return done(last)
        if (providers.isEmpty() || Build.VERSION.SDK_INT < 30) return done(last)

        val ui = Handler(Looper.getMainLooper())
        val cancel = CancellationSignal()
        var finished = false
        val finish = { loc: Location? -> if (!finished) { finished = true; done(loc) } }
        ui.postDelayed({ cancel.cancel(); finish(last) }, WAIT_MS)
        lm.getCurrentLocation(providers.first(), cancel, ctx.mainExecutor) { loc -> finish(loc ?: last) }
    }
}
