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

    fun saveCookies(
        context: Context,
        cookies: String,
        domain: String = ".instagram.com",
    ) {
        val file = cookieFile(context)
        file.parentFile?.mkdirs()

        // CookieManager returns "name1=value1; name2=value2"
        val entries =
            cookies
                .split("; ")
                .mapNotNull { pair ->
                    val eq = pair.indexOf('=')
                    if (eq <= 0) return@mapNotNull null
                    val name = pair.substring(0, eq).trim()
                    val value = pair.substring(eq + 1).trim()
                    if (name.isBlank()) return@mapNotNull null
                    // Netscape format:
                    // domain  flag  path  secure  expiration  name  value
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
