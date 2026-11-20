package ge.elderfriend.launcher

import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.ContactsContract

/** People the caregiver pinned; shown on the Favourites screen, no limit. */
object Favourites {

    private const val KEY = "favourites"

    data class Fav(val contactId: Long, val name: String, val number: String)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("elder", Context.MODE_PRIVATE)

    // One per line: id|name|number. A '|' or newline in a name is replaced so it can't split a row.
    fun list(ctx: Context): List<Fav> = prefs(ctx).getString(KEY, "").orEmpty().lines().mapNotNull { line ->
        val p = line.split('|')
        if (p.size == 3) Fav(p[0].toLongOrNull() ?: 0L, p[1], p[2]) else null
    }

    private fun save(ctx: Context, favs: List<Fav>) {
        prefs(ctx).edit().putString(KEY, favs.joinToString("\n") {
            "${it.contactId}|${it.name.replace(Regex("[|\\n]"), " ")}|${it.number}"
        }).apply()
    }

    fun set(ctx: Context, favs: List<Fav>) = save(ctx, favs)

    /** Every contact with a number, one row per name, alphabetical. */
    fun all(ctx: Context): List<Fav> {
        val found = LinkedHashMap<String, Fav>()
        try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1)?.trim().orEmpty()
                    val number = c.getString(2)?.trim().orEmpty()
                    if (name.isNotEmpty() && number.isNotEmpty() && name !in found) found[name] = Fav(c.getLong(0), name, number)
                }
            }
        } catch (e: Exception) {
            // No permission or provider: empty picker.
        }
        return found.values.toList()
    }

    fun photo(ctx: Context, fav: Fav): Drawable? = try {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, fav.contactId)
        ContactsContract.Contacts.openContactPhotoInputStream(ctx.contentResolver, uri, true)?.use {
            BitmapDrawable(ctx.resources, it)
        }
    } catch (e: Exception) {
        null
    }
}
