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
}
