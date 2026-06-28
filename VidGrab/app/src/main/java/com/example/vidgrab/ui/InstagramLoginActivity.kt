package com.example.vidgrab.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.vidgrab.R
import com.example.vidgrab.util.CookieStorage

class InstagramLoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instagram_login)
        title = getString(R.string.login_instagram_title)

        val webView = findViewById<WebView>(R.id.webview)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeAllCookies(null)

        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith(INSTAGRAM_HOME)) {
                        saveCookiesAndFinish()
                        return true
                    }
                    return false
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?,
                ) {
                    super.onPageFinished(view, url)
                    if (url != null && url.startsWith(INSTAGRAM_HOME)) {
                        saveCookiesAndFinish()
                    }
                }
            }

        webView.loadUrl(LOGIN_URL)
    }

    private fun saveCookiesAndFinish() {
        val cookies = CookieManager.getInstance().getCookie(INSTAGRAM_HOME) ?: ""
        if (cookies.isNotBlank()) {
            CookieStorage.saveCookies(this, cookies)
            if (CookieStorage.hasSession(this)) {
                setResult(Activity.RESULT_OK)
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        private const val LOGIN_URL = "https://www.instagram.com/accounts/login/"
        private const val INSTAGRAM_HOME = "https://www.instagram.com/"

        fun newIntent(context: android.content.Context): Intent = Intent(context, InstagramLoginActivity::class.java)
    }
}
