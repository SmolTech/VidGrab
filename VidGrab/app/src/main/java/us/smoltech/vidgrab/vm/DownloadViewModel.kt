package us.smoltech.vidgrab.vm

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.smoltech.vidgrab.service.DownloadForegroundService
import us.smoltech.vidgrab.util.CookieStorage
import us.smoltech.vidgrab.util.DownloadResult
import us.smoltech.vidgrab.util.DownloadUiState

class DownloadViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _ytDlpVersion = MutableStateFlow<String?>(null)
    val ytDlpVersion: StateFlow<String?> = _ytDlpVersion.asStateFlow()

    private val resultReceiver =
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(
                resultCode: Int,
                resultData: Bundle?,
            ) {
                when (resultCode) {
                    DownloadForegroundService.RESULT_START -> {
                        val url = resultData?.getString(DownloadForegroundService.EXTRA_URL).orEmpty()
                        _uiState.value = DownloadUiState(url = url, result = DownloadResult.Starting(url), isDownloading = true)
                    }

                    DownloadForegroundService.RESULT_PROGRESS -> {
                        val progress =
                            DownloadResult.Progress(
                                percent = resultData?.getFloat(DownloadForegroundService.EXTRA_PERCENT) ?: 0f,
                                downloaded = resultData?.getLong(DownloadForegroundService.EXTRA_DOWNLOADED) ?: 0L,
                                total = resultData?.getLong(DownloadForegroundService.EXTRA_TOTAL) ?: 0L,
                                speed = resultData?.getLong(DownloadForegroundService.EXTRA_SPEED) ?: 0L,
                                eta = resultData?.getInt(DownloadForegroundService.EXTRA_ETA) ?: 0,
                                filename = resultData?.getString(DownloadForegroundService.EXTRA_FILENAME).orEmpty(),
                            )
                        _uiState.value = _uiState.value.copy(result = progress)
                    }

                    DownloadForegroundService.RESULT_CONVERTING -> {
                        val filename = resultData?.getString(DownloadForegroundService.EXTRA_FILENAME).orEmpty()
                        _uiState.value = _uiState.value.copy(result = DownloadResult.Converting(filename))
                    }

                    DownloadForegroundService.RESULT_COMPLETE -> {
                        val file = resultData?.getString(DownloadForegroundService.EXTRA_FILE).orEmpty()
                        _uiState.value = _uiState.value.copy(result = DownloadResult.Complete(file), isDownloading = false)
                    }

                    DownloadForegroundService.RESULT_ERROR -> {
                        val message = resultData?.getString(DownloadForegroundService.EXTRA_MESSAGE).orEmpty()
                        _uiState.value = _uiState.value.copy(result = DownloadResult.Error(message), isDownloading = false)
                    }
                }
            }
        }

    fun loadYtDlpVersion(context: Context) {
        if (_ytDlpVersion.value != null) return
        viewModelScope.launch {
            _ytDlpVersion.value = queryYtDlpVersion(context.applicationContext)
        }
    }

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url)
    }

    fun startDownload(context: Context) {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return
        val options =
            if (CookieStorage.hasSession(context)) {
                Bundle().apply {
                    putString("cookiefile", CookieStorage.cookieFile(context).absolutePath)
                    CookieStorage.userAgent(context)?.let { putString("user_agent", it) }
                }
            } else {
                null
            }
        DownloadForegroundService.start(context, url, resultReceiver, options)
    }

    fun reset() {
        _uiState.value = DownloadUiState(url = _uiState.value.url)
    }

    fun onErrorShown() {
        _uiState.value = _uiState.value.copy(result = DownloadResult.Idle)
    }

    private suspend fun queryYtDlpVersion(context: Context): String =
        withContext(Dispatchers.IO) {
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context))
                }
                Python
                    .getInstance()
                    .getModule("downloader")
                    .callAttr("get_version")
                    .toString()
            } catch (e: Exception) {
                ""
            }
        }
}
