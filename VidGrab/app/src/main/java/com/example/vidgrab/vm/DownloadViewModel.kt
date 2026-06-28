package com.example.vidgrab.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vidgrab.service.DownloadForegroundService
import com.example.vidgrab.util.DownloadStateManager
import com.example.vidgrab.util.DownloadUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DownloadViewModel : ViewModel() {

    val uiState: StateFlow<DownloadUiState> = DownloadStateManager.state

    fun onUrlChange(url: String) {
        DownloadStateManager.setUrl(url)
    }

    fun startDownload(context: Context) {
        val url = uiState.value.url.trim()
        if (url.isBlank()) return
        DownloadForegroundService.start(context, url)
    }

    fun reset() {
        DownloadStateManager.reset()
    }
}
