package ge.elderfriend.launcher

import android.content.Context

/** Which of the six fixed tiles are currently shown on the home screen. */
class TileStore(ctx: Context) {

    private val prefs =
        ctx.applicationContext.getSharedPreferences("elder", Context.MODE_PRIVATE)

    fun visible(id: String): Boolean = prefs.getBoolean("tile_$id", true)

    fun setVisible(id: String, visible: Boolean) {
        prefs.edit().putBoolean("tile_$id", visible).apply()
    }

    companion object {
        /** Order matters: this is the on-screen order, 2 per row. */
        val ALL = listOf(
            "call", "contacts", "missed", "recent", "camera", "messages",
            "facebook", "messenger", "whatsapp", "viber"
        )
    }
}
