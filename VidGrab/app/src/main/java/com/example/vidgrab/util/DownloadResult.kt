package com.example.vidgrab.util

sealed class DownloadResult {
    data object Idle : DownloadResult()

    data class Starting(
        val url: String,
    ) : DownloadResult()

    data class Progress(
        val percent: Float,
        val downloaded: Long,
        val total: Long,
        val speed: Long,
        val eta: Int,
        val filename: String,
    ) : DownloadResult()

    data class Converting(
        val filename: String,
    ) : DownloadResult()

    data class Complete(
        val file: String,
    ) : DownloadResult()

    data class Error(
        val message: String,
    ) : DownloadResult()
}

data class DownloadUiState(
    val url: String = "",
    val result: DownloadResult = DownloadResult.Idle,
    val isDownloading: Boolean = false,
)
