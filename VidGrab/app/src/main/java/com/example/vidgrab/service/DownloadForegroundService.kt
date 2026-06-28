package com.example.vidgrab.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.vidgrab.R
import com.example.vidgrab.util.DownloadResult
import com.example.vidgrab.util.MediaStoreHelper
import com.example.vidgrab.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class DownloadForegroundService : LifecycleService() {
    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_RESULT_RECEIVER = "extra_result_receiver"

        const val RESULT_START = 1
        const val RESULT_PROGRESS = 2
        const val RESULT_CONVERTING = 3
        const val RESULT_COMPLETE = 4
        const val RESULT_ERROR = 5

        const val EXTRA_PERCENT = "percent"
        const val EXTRA_DOWNLOADED = "downloaded"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_ETA = "eta"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_FILE = "file"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_OPTIONS = "extra_options"

        fun start(
            context: Context,
            url: String,
            resultReceiver: ResultReceiver,
            options: Bundle? = null,
        ) {
            val intent =
                Intent(context, DownloadForegroundService::class.java).apply {
                    putExtra(EXTRA_URL, url)
                    putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
                    putExtra(EXTRA_OPTIONS, options)
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
        val resultReceiver = intent?.getParcelableExtra<ResultReceiver>(EXTRA_RESULT_RECEIVER)
        val options = intent?.getBundleExtra(EXTRA_OPTIONS)

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
        resultReceiver?.send(RESULT_START, Bundle().apply { putString(EXTRA_URL, url) })

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                runDownload(url, resultReceiver, options)
            } finally {
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private suspend fun runDownload(
        url: String,
        resultReceiver: ResultReceiver?,
        options: Bundle?,
    ) {
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
                    resultReceiver?.send(
                        RESULT_PROGRESS,
                        Bundle().apply {
                            putFloat(EXTRA_PERCENT, percent.toFloat())
                            putLong(EXTRA_DOWNLOADED, downloaded)
                            putLong(EXTRA_TOTAL, total)
                            putLong(EXTRA_SPEED, speed)
                            putInt(EXTRA_ETA, eta)
                            putString(EXTRA_FILENAME, filename)
                        },
                    )
                }

                override fun onConverting(filename: String) {
                    updateNotification(
                        title = getString(R.string.notification_download_converting),
                        content = File(filename).name,
                        progress = NotificationHelper.INDETERMINATE_PROGRESS,
                    )
                    resultReceiver?.send(
                        RESULT_CONVERTING,
                        Bundle().apply { putString(EXTRA_FILENAME, filename) },
                    )
                }

                override fun onComplete(file: String) {
                    // Completion is handled after Python returns so we can copy to MediaStore.
                }

                override fun onError(message: String) {
                    resultReceiver?.send(
                        RESULT_ERROR,
                        Bundle().apply { putString(EXTRA_MESSAGE, message) },
                    )
                }
            }

        try {
            val pythonOptions = options?.getString("cookiefile")?.let { mapOf("cookiefile" to it) }
            val resultJson = module.callAttr("download", url, outDir, pythonOptions, callback).toString()
            val result = JSONObject(resultJson)
            val status = result.getString("status")
            val filePath = result.optString("file", "").takeIf { it.isNotEmpty() }
            val message = result.getString("message")

            if (status == "ok" && filePath != null) {
                val sourceFile = File(filePath)
                val uri = MediaStoreHelper.insertVideo(this, sourceFile)

                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        resultReceiver?.send(
                            RESULT_COMPLETE,
                            Bundle().apply { putString(EXTRA_FILE, uri.toString()) },
                        )
                        updateNotification(
                            title = getString(R.string.notification_download_complete),
                            content = getString(R.string.notification_download_saved),
                            progress = 100,
                            ongoing = false,
                        )
                    } else {
                        resultReceiver?.send(
                            RESULT_COMPLETE,
                            Bundle().apply { putString(EXTRA_FILE, sourceFile.absolutePath) },
                        )
                        resultReceiver?.send(
                            RESULT_ERROR,
                            Bundle().apply { putString(EXTRA_MESSAGE, getString(R.string.error_media_store)) },
                        )
                    }
                }
                sourceFile.delete()
            } else {
                resultReceiver?.send(
                    RESULT_ERROR,
                    Bundle().apply { putString(EXTRA_MESSAGE, message) },
                )
            }
        } catch (e: Exception) {
            resultReceiver?.send(
                RESULT_ERROR,
                Bundle().apply { putString(EXTRA_MESSAGE, e.message ?: getString(R.string.error_unknown)) },
            )
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
