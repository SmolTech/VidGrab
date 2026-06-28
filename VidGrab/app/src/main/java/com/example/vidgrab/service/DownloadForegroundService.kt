package com.example.vidgrab.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.vidgrab.R
import com.example.vidgrab.util.DownloadResult
import com.example.vidgrab.util.DownloadStateManager
import com.example.vidgrab.util.MediaStoreHelper
import com.example.vidgrab.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadForegroundService : LifecycleService() {
    companion object {
        private const val EXTRA_URL = "extra_url"

        fun start(
            context: Context,
            url: String,
        ) {
            val intent =
                Intent(context, DownloadForegroundService::class.java).apply {
                    putExtra(EXTRA_URL, url)
                }
            context.startForegroundService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }

        val initialNotification =
            NotificationHelper
                .buildNotification(
                    context = this,
                    title = getString(R.string.notification_download_starting),
                    content = url,
                    progress = 0,
                ).build()

        startForeground(NotificationHelper.NOTIFICATION_ID, initialNotification)
        DownloadStateManager.start(url)

        lifecycleScope.launch(Dispatchers.IO) {
            runDownload(url)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private suspend fun runDownload(url: String) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val python = Python.getInstance()
        val module = python.getModule("downloader")
        val outDir = File(cacheDir, "downloads").absolutePath

        val callback =
            object : DownloadCallback {
                override fun onStart(url: String) {
                    updateNotification(
                        title = getString(R.string.notification_download_starting),
                        content = url,
                        progress = 0,
                    )
                }

                override fun onProgress(
                    percent: Double,
                    downloaded: Long,
                    total: Long,
                    speed: Long,
                    eta: Int,
                    filename: String,
                ) {
                    val progress = percent.toInt()
                    updateNotification(
                        title = getString(R.string.notification_download_progress, progress),
                        content = File(filename).name,
                        progress = progress,
                    )
                    DownloadStateManager.setProgress(
                        DownloadResult.Progress(
                            percent = percent.toFloat(),
                            downloaded = downloaded,
                            total = total,
                            speed = speed,
                            eta = eta,
                            filename = filename,
                        ),
                    )
                }

                override fun onConverting(filename: String) {
                    updateNotification(
                        title = getString(R.string.notification_download_converting),
                        content = File(filename).name,
                        progress = -1,
                    )
                    DownloadStateManager.setConverting(filename)
                }

                override fun onComplete(file: String) {
                    // Handled after Python returns so we can copy to MediaStore.
                }

                override fun onError(message: String) {
                    DownloadStateManager.setError(message)
                }
            }

        try {
            val resultJson = module.callAttr("download", url, outDir, null, callback).toString()
            val result = org.json.JSONObject(resultJson)
            val status = result.getString("status")
            val filePath = result.optString("file", "").takeIf { it.isNotEmpty() }
            val message = result.getString("message")

            if (status == "ok" && filePath != null) {
                val sourceFile = File(filePath)
                val uri = MediaStoreHelper.insertVideo(this, sourceFile)

                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        DownloadStateManager.setComplete(uri.toString())
                        updateNotification(
                            title = getString(R.string.notification_download_complete),
                            content = getString(R.string.notification_download_saved),
                            progress = 100,
                            ongoing = false,
                        )
                    } else {
                        // MediaStore failed; keep the file in cache and report its path.
                        DownloadStateManager.setComplete(sourceFile.absolutePath)
                        DownloadStateManager.setError(getString(R.string.error_media_store))
                    }
                }
                sourceFile.delete()
            } else {
                withContext(Dispatchers.Main) {
                    DownloadStateManager.setError(message ?: getString(R.string.error_unknown))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                DownloadStateManager.setError(e.message ?: getString(R.string.error_unknown))
            }
        }
    }

    private fun updateNotification(
        title: String,
        content: String,
        progress: Int,
        ongoing: Boolean = true,
    ) {
        val builder =
            NotificationHelper
                .buildNotification(this, title, content, progress)
                .setOngoing(ongoing)
        if (!ongoing) {
            builder.setProgress(0, 0, false)
        }
        NotificationManagerCompat.from(this).notify(NotificationHelper.NOTIFICATION_ID, builder.build())
    }
}
