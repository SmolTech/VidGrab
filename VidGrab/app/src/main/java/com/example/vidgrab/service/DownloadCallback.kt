package com.example.vidgrab.service

/**
 * Callback interface invoked from Python/yt-dlp during a download.
 * Method names use camelCase because Python calls them directly on the Java object.
 */
interface DownloadCallback {
    fun onStart(url: String)

    fun onProgress(
        percent: Double,
        downloaded: Long,
        total: Long,
        speed: Long,
        eta: Int,
        filename: String,
    )

    fun onConverting(filename: String)

    fun onComplete(file: String)

    fun onError(message: String)
}
