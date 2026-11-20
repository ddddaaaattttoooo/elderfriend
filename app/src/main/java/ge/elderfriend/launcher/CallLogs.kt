package ge.elderfriend.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog

/** Small, read-only helpers for the system call log. */
object CallLogs {

    /** Missed calls newer than [since]; 0 when the log is not readable. */
    fun missedSince(ctx: Context, since: Long): Int {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) return 0
        return try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), since.toString()),
                null
            )?.use { it.count } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    data class Missed(val name: String, val number: String, val date: Long)

    /** Missed calls after [since], newest first. Name falls back to the number. */
    fun missedCalls(ctx: Context, since: Long): List<Missed> {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) return emptyList()
        return try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), since.toString()),
                "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                val out = ArrayList<Missed>()
                while (c.moveToNext()) {
                    val number = c.getString(1).orEmpty()
                    if (number.isNotBlank()) {
                        out += Missed(c.getString(0)?.takeIf { it.isNotBlank() } ?: number, number, c.getLong(2))
                    }
                }
                out
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
