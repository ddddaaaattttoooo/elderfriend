package ge.elderfriend.launcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/** Second screen from the Favourites tile: one big tile per pinned person. */
class FavouritesActivity : Activity() {

    override fun onResume() {
        super.onResume()
        val pad = Ui.dp(this, 8)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_home)
            setPadding(pad, Ui.dp(this@FavouritesActivity, 10), pad, pad)
        }
        root.addView(
            Ui.text(this, getString(R.string.tile_favourites), 28f, Color.parseColor("#FFB9C6D2")),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (fav in Favourites.list(this)) {
            list.addView(
                Ui.tile(this, fav.name, Color.parseColor("#FF0E7490"), Favourites.photo(this, fav), subtitle = fav.number) {
                    Ui.confirm(this, getString(R.string.confirm_call, fav.name), fav.number) {
                        Apps.call(this, fav.number)
                    }
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)).also {
                    it.setMargins(0, pad, 0, 0)
                }
            )
        }
        root.addView(
            ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        root.addView(
            Ui.bigButton(this, getString(R.string.home), Color.parseColor("#FF2A2A2A"), 30f, 96) { finish() },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.setMargins(0, pad, 0, 0)
            }
        )
        setContentView(root)
    }
}
