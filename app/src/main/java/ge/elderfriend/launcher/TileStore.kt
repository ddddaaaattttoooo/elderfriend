package ge.elderfriend.launcher

import android.content.Context

/**
 * What the home screen shows: the built-in tiles (each can be hidden) and the
 * apps the caregiver picked, in the order they were picked.
 */
class TileStore(ctx: Context) {

    private val ctx = ctx.applicationContext
    private val prefs = this.ctx.getSharedPreferences("elder", Context.MODE_PRIVATE)

    fun visible(id: String): Boolean = prefs.getBoolean("tile_$id", true)

    fun setVisible(id: String, visible: Boolean) {
        prefs.edit().putBoolean("tile_$id", visible).apply()
    }

    /** Package names of the chosen apps, in home-screen order. */
    fun apps(): List<String> {
        if (!prefs.contains(KEY_APPS)) setApps(legacyApps())
        return prefs.getString(KEY_APPS, "").orEmpty().split(',').filter { it.isNotBlank() }
    }

    fun setApps(packages: List<String>) {
        prefs.edit().putString(KEY_APPS, packages.distinct().joinToString(",")).apply()
    }

    /**
     * 1.0.x always showed WhatsApp, Viber, Messenger and Facebook when they were
     * installed and not hidden. Keep exactly that on upgrade so a phone that is
     * already in use looks the same after the update.
     */
    private fun legacyApps(): List<String> = LEGACY
        .filter { (id, pkg) -> visible(id) && Apps.isInstalled(ctx, pkg) }
        .map { it.second }

    companion object {
        private const val KEY_APPS = "apps"

        /** Order matters: this is the on-screen order, 2 per row. */
        val BUILT_IN = listOf("call", "contacts", "missed", "recent", "camera", "messages")

        private val LEGACY = listOf(
            "facebook" to Apps.FACEBOOK,
            "messenger" to Apps.MESSENGER,
            "whatsapp" to Apps.WHATSAPP,
            "viber" to Apps.VIBER
        )
    }
}
