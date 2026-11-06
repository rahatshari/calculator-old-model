package com.calculator.landwood

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.displayZoomControls = false
        settings.builtInZoomControls = false

        // Bridge for In-App Auto Updates and Native Communication
        webView.addJavascriptInterface(AndroidUpdater(this, webView), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith("file:///android_asset/")) {
                    return false
                }
                // Open external links (like github releases) in external browser if clicked
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        return false
                    }
                }
                return false
            }
        }
        webView.webChromeClient = WebChromeClient()

        // Load 100% offline bundled HTML application
        webView.loadUrl("file:///android_asset/index.html")

        // Smooth Android system back button handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    class AndroidUpdater(private val activity: Activity, private val webView: WebView) {

        @JavascriptInterface
        fun isNativeApp(): Boolean = true

        @JavascriptInterface
        fun getAppVersion(): String {
            return try {
                val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                pInfo.versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }
        }

        @JavascriptInterface
        fun downloadAndInstallApk(downloadUrl: String) {
            Thread {
                try {
                    activity.runOnUiThread {
                        webView.evaluateJavascript("if(window.onUpdateProgress) window.onUpdateProgress('ডাউনলোড শুরু হচ্ছে...');", null)
                    }

                    var connection = URL(downloadUrl).openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 10000
                    connection.readTimeout = 25000

                    var redirectCount = 0
                    while (connection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 5) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        connection = URL(newUrl).openConnection() as HttpURLConnection
                        connection.connectTimeout = 10000
                        connection.readTimeout = 25000
                        redirectCount++
                    }

                    if (connection.responseCode != 200) {
                        activity.runOnUiThread {
                            val msg = "সার্ভার রেসপন্স: ${connection.responseCode}"
                            webView.evaluateJavascript("if(window.onUpdateError) window.onUpdateError('$msg');", null)
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }

                    val cacheDir = File(activity.cacheDir, "updates")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val apkFile = File(cacheDir, "calculator-update.apk")

                    val totalLength = connection.contentLength
                    connection.inputStream.use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloaded: Long = 0
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                if (totalLength > 0) {
                                    val percent = (downloaded * 100 / totalLength).toInt()
                                    activity.runOnUiThread {
                                        webView.evaluateJavascript("if(window.onUpdateProgress) window.onUpdateProgress('ডাউনলোড হচ্ছে: ${percent}%');", null)
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    activity.runOnUiThread {
                        webView.evaluateJavascript("if(window.onUpdateProgress) window.onUpdateProgress('ইনস্টলেশন শুরু হচ্ছে...');", null)
                        installApk(apkFile)
                    }

                } catch (e: Exception) {
                    activity.runOnUiThread {
                        val err = e.message ?: "ডাউনলোডে ত্রুটি"
                        webView.evaluateJavascript("if(window.onUpdateError) window.onUpdateError('${err.replace("'", "\\'")}');", null)
                        Toast.makeText(activity, "আপডেট ত্রুটি: $err", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        private fun installApk(file: File) {
            try {
                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "ইনস্টলার খুলতে সমস্যা: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
