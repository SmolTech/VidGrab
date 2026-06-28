package com.example.vidgrab.util

import android.content.Context
import java.io.File

/**
 * Stores and retrieves Instagram session cookies in Netscape format for yt-dlp.
 */
object CookieStorage {
    private const val COOKIE_DIR = "cookies"
    private const val COOKIE_FILE = "instagram.txt"

    fun cookieFile(context: Context): File = File(context.cacheDir, "$COOKIE_DIR/$COOKIE_FILE")

    fun hasCookies(context: Context): Boolean = cookieFile(context).exists()

    /**
     * Returns true if the stored cookies contain a session ID, which indicates
     * the user is logged in.
     */
    fun hasSession(context: Context): Boolean {
        val file = cookieFile(context)
        if (!file.exists()) return false
        return file.readLines().any { it.contains("\tsessionid\t") }
    }

    fun saveCookies(
        context: Context,
        cookies: String,
    ) {
        val file = cookieFile(context)
        file.parentFile?.mkdirs()

        val pairs =
            cookies
                .split("; ")
                .mapNotNull { pair ->
                    val eq = pair.indexOf('=')
                    if (eq <= 0) return@mapNotNull null
                    val name = pair.substring(0, eq).trim()
                    val value = pair.substring(eq + 1).trim()
                    if (name.isBlank()) return@mapNotNull null
                    name to value
                }

        if (pairs.isEmpty()) return

        // yt-dlp matches cookies by domain. Instagram sets cookies for
        // .instagram.com (domain-wide) and sometimes www.instagram.com.
        // Write entries for both to maximize compatibility.
        val domains = listOf(".instagram.com", "www.instagram.com", "instagram.com")

        val entries =
            domains.flatMap { domain ->
                pairs.map { (name, value) ->
                    listOf(
                        domain,
                        "TRUE",
                        "/",
                        "FALSE",
                        "0",
                        name,
                        value,
                    ).joinToString("\t")
                }
            }

        file.writeText(
            buildString {
                appendLine("# Netscape HTTP Cookie File")
                entries.forEach { appendLine(it) }
            },
        )
    }

    fun clear(context: Context) {
        cookieFile(context).delete()
    }
}
