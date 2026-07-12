package us.smoltech.vidgrab.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import us.smoltech.vidgrab.R
import us.smoltech.vidgrab.util.CookieStorage

class InstagramLoginActivity : Activity() {
    private lateinit var webView: WebView
    private var hasFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instagram_login)
        title = getString(R.string.login_instagram_title)

        webView = findViewById(R.id.webview)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeAllCookies(null)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            // Use a desktop Chrome UA so Instagram doesn't block the WebView and so the
            // login session fingerprint matches what yt-dlp will send later.
            userAgentString = DESKTOP_USER_AGENT
        }
        CookieStorage.saveUserAgent(this, DESKTOP_USER_AGENT)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    Log.d(TAG, "Navigating to: $url")
                    if (isInstagramHome(url)) {
                        scheduleFinish()
                        return true
                    }
                    return false
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?,
                ) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished: $url")
                    if (url != null && isInstagramHome(url)) {
                        scheduleFinish()
                    }
                }
            }

        webView.loadUrl(LOGIN_URL)
    }

    private fun scheduleFinish() {
        if (hasFinished) return
        hasFinished = true
        // Cookies may arrive shortly after the redirect. Wait briefly and retry a few times.
        trySaveCookies(attempt = 0)
    }

    private fun trySaveCookies(attempt: Int) {
        val manager = CookieManager.getInstance()
        val cookies = manager.getCookie(INSTAGRAM_HOME) ?: ""
        val rawInstagram = manager.getCookie("https://instagram.com/") ?: ""
        Log.d(TAG, "Attempt $attempt - www cookies length: ${cookies.length}, instagram.com length: ${rawInstagram.length}")

        val combined = listOf(cookies, rawInstagram).filter { it.isNotBlank() }.joinToString("; ")

        if (combined.isNotBlank()) {
            CookieStorage.saveCookies(this, combined)
        }

        if (CookieStorage.hasSession(this) || attempt >= MAX_ATTEMPTS) {
            val hasSession = CookieStorage.hasSession(this)
            Log.d(TAG, "Finishing. Has sessionid: $hasSession")
            setResult(if (hasSession) Activity.RESULT_OK else Activity.RESULT_CANCELED)
            finish()
        } else {
            webView.postDelayed({ trySaveCookies(attempt + 1) }, RETRY_DELAY_MS)
        }
    }

    private fun isInstagramHome(url: String): Boolean =
        url.startsWith(INSTAGRAM_HOME) ||
            url.equals("https://instagram.com/", ignoreCase = true) ||
            url.equals("https://instagram.com", ignoreCase = true)

    companion object {
        private const val TAG = "InstagramLogin"
        private const val LOGIN_URL = "https://www.instagram.com/accounts/login/"
        private const val INSTAGRAM_HOME = "https://www.instagram.com/"
        private const val MAX_ATTEMPTS = 5
        private const val RETRY_DELAY_MS = 800L

        // A current desktop Chrome UA. Keep this in sync with the version passed to yt-dlp.
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        fun newIntent(context: android.content.Context): Intent = Intent(context, InstagramLoginActivity::class.java)
    }
}
