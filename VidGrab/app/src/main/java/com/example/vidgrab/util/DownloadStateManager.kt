package com.example.vidgrab.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DownloadStateManager {
    private val _state = MutableStateFlow(DownloadUiState())
    val state: StateFlow<DownloadUiState> = _state.asStateFlow()

    fun setUrl(url: String) {
        _state.update { it.copy(url = url) }
    }

    fun start(url: String) {
        _state.update { DownloadUiState(url = url, result = DownloadResult.Starting(url), isDownloading = true) }
    }

    fun setProgress(progress: DownloadResult.Progress) {
        _state.update { it.copy(result = progress) }
    }

    fun setConverting(filename: String) {
        _state.update { it.copy(result = DownloadResult.Converting(filename)) }
    }

    fun setComplete(file: String) {
        _state.update { it.copy(result = DownloadResult.Complete(file), isDownloading = false) }
    }

    fun setError(message: String) {
        _state.update { it.copy(result = DownloadResult.Error(message), isDownloading = false) }
    }

    fun reset() {
        _state.update { DownloadUiState(url = it.url) }
    }
}
