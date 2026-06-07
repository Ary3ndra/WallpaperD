package org.piarsenal.wallchanger.log

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiny append-only file logger so failures (downloads, API errors, apply errors) leave an exact,
 * user-viewable record instead of vanishing. Capped in size; also mirrors to logcat.
 */
object Logger {

    private const val MAX_BYTES = 256 * 1024L
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    private fun file(context: Context): File =
        File(context.filesDir, "logs/app.log").apply { parentFile?.mkdirs() }

    @Synchronized
    fun log(context: Context, level: String, tag: String, msg: String, t: Throwable? = null) {
        val line = buildString {
            append(fmt.format(Date())).append(' ').append(level).append('/').append(tag)
            append(": ").append(msg)
            if (t != null) append("  [").append(t.javaClass.simpleName).append(": ").append(t.message).append(']')
        }
        android.util.Log.println(
            if (level == "E") android.util.Log.ERROR else android.util.Log.INFO, tag, msg
        )
        runCatching {
            val f = file(context)
            if (f.length() > MAX_BYTES) {
                val keep = f.readText().takeLast((MAX_BYTES / 2).toInt())
                f.writeText(keep)
            }
            f.appendText(line + "\n")
        }
    }

    fun i(context: Context, tag: String, msg: String) = log(context, "I", tag, msg)
    fun w(context: Context, tag: String, msg: String) = log(context, "W", tag, msg)
    fun e(context: Context, tag: String, msg: String, t: Throwable? = null) = log(context, "E", tag, msg, t)

    /** Most recent log text (tail), newest last. */
    fun recent(context: Context, maxChars: Int = 8000): String = runCatching {
        val f = file(context)
        if (!f.exists()) "" else f.readText().takeLast(maxChars)
    }.getOrDefault("")

    fun clear(context: Context) {
        runCatching { file(context).writeText("") }
    }
}
