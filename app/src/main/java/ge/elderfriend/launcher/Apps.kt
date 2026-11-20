package ge.elderfriend.launcher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast

/** All outbound intents live here so the failure paths are in one place. */
object Apps {

    const val WHATSAPP = "com.whatsapp"
    const val VIBER = "com.viber.voip"
    const val MESSENGER = "com.facebook.orca"
    const val FACEBOOK = "com.facebook.katana"
    const val AURORA = "com.aurora.store"
    const val PLAY_STORE = "com.android.vending"
    const val STOCK_HOME = "com.android.launcher3"
    const val PHOTOS = "com.google.android.apps.photos"

    /** Installed by phase2/install.sh; Fossify builds from the F-Droid repo. */
    const val FOSSIFY_GALLERY = "org.fossify.gallery"
    const val FOSSIFY_PHONE = "org.fossify.phone"

    fun isInstalled(ctx: Context, pkg: String): Boolean = try {
        ctx.packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (e: Exception) {
        false
    }

    /** The app's own launcher icon — the logo the user already recognises. */
    fun icon(ctx: Context, pkg: String): Drawable? = try {
        ctx.packageManager.getApplicationIcon(pkg)
    } catch (e: Exception) {
        null
    }

    /** The name the app shows under its own icon. */
    fun label(ctx: Context, pkg: String): String = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    /** Every app with a launcher icon except Elderfriend itself, sorted by name. */
    fun launchable(ctx: Context): List<String> {
        val pm = ctx.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .map { it.activityInfo.packageName }
            .filter { it != ctx.packageName }
            .distinct()
            .sortedBy { label(ctx, it).lowercase() }
    }

    fun launch(ctx: Context, pkg: String): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchOrSay(activity: Activity, pkg: String, label: String) {
        if (!launch(activity, pkg)) {
            Ui.info(activity, activity.getString(R.string.not_installed, label))
        }
    }

    /**
     * Direct call when CALL_PHONE is granted, otherwise the dialer with the
     * number pre-filled. Never fails silently.
     */
    fun call(ctx: Context, number: String) {
        val granted = ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val intent = Intent(
            if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.parse("tel:" + Uri.encode(number))
        )
        if (ctx !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, R.string.unknown, Toast.LENGTH_LONG).show()
        }
    }

    fun emergency(ctx: Context) = call(ctx, "112")

    private fun start(activity: Activity, intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    fun openSms(activity: Activity) {
        val sms = Intent(Intent.ACTION_VIEW).apply { type = "vnd.android-dir/mms-sms" }
        try {
            activity.startActivity(sms)
            return
        } catch (e: Exception) {
            // fall through
        }
        try {
            activity.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
            )
        } catch (e: Exception) {
            Ui.info(activity, activity.getString(R.string.unknown))
        }
    }

    fun openContacts(activity: Activity) {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS)
            )
        } catch (e: Exception) {
            try {
                activity.startActivity(Intent(Intent.ACTION_DIAL))
            } catch (e2: Exception) {
                Ui.info(activity, activity.getString(R.string.unknown))
            }
        }
    }

    /** Stock camera in still-picture mode; falls back to the capture intent. */
    fun takePhoto(activity: Activity) {
        try {
            activity.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            return
        } catch (e: Exception) {
            // fall through
        }
        try {
            activity.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        } catch (e: Exception) {
            Ui.info(activity, activity.getString(R.string.no_camera))
        }
    }

    fun openPhotos(activity: Activity) {
        // Fossify Gallery is the gallery we install and support.
        if (isInstalled(activity, FOSSIFY_GALLERY) && launch(activity, FOSSIFY_GALLERY)) return
        if (isInstalled(activity, PHOTOS) && launch(activity, PHOTOS)) return
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).apply { type = "image/*" })
        } catch (e: Exception) {
            Ui.info(activity, activity.getString(R.string.unknown))
        }
    }
}
