package us.smoltech.vidgrab.ui

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import us.smoltech.vidgrab.R
import us.smoltech.vidgrab.service.DownloadForegroundService
import us.smoltech.vidgrab.util.CookieStorage
import us.smoltech.vidgrab.util.DownloadResult
import us.smoltech.vidgrab.vm.DownloadViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: DownloadViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val ytDlpVersion by viewModel.ytDlpVersion.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var snackbarShown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadYtDlpVersion(context)
    }

    var isLoggedIn by remember { mutableStateOf(CookieStorage.hasSession(context)) }

    val loginLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                isLoggedIn = true
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            // Even if denied, we can still start the service; Android will just not show the notification.
            viewModel.startDownload(context)
        }

    LaunchedEffect(uiState.result) {
        val result = uiState.result
        if (result is DownloadResult.Error && !snackbarShown) {
            snackbarHostState.showSnackbar(result.message)
            snackbarShown = true
        } else if (result !is DownloadResult.Error) {
            snackbarShown = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.instructions),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text(stringResource(R.string.hint_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                viewModel.onUrlChange(text)
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_paste),
                            contentDescription = stringResource(R.string.paste),
                        )
                    }
                },
            )

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                            android.content.pm.PackageManager.PERMISSION_GRANTED -> viewModel.startDownload(context)
                            else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.startDownload(context)
                    }
                },
                enabled = !uiState.isDownloading && uiState.url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.download))
            }

            if (uiState.isDownloading) {
                Button(
                    onClick = { DownloadForegroundService.cancel(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }

            Text(
                text =
                    if (isLoggedIn) {
                        stringResource(R.string.instagram_logged_in)
                    } else {
                        stringResource(R.string.instagram_not_logged_in)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    if (isLoggedIn) {
                        CookieStorage.clear(context)
                        isLoggedIn = false
                    } else {
                        loginLauncher.launch(InstagramLoginActivity.newIntent(context))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (isLoggedIn) {
                            R.string.logout_instagram
                        } else {
                            R.string.login_instagram
                        },
                    ),
                )
            }

            DownloadStatus(
                result = uiState.result,
                onDismiss = { viewModel.onErrorShown() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.powered_by, ytDlpVersion ?: stringResource(R.string.version_unknown)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadStatus(
    result: DownloadResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (result) {
            is DownloadResult.Idle -> { /* nothing */ }

            is DownloadResult.Starting -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.status_starting))
            }

            is DownloadResult.Progress -> {
                LinearProgressIndicator(
                    progress = { result.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        R.string.status_progress,
                        result.percent.toInt(),
                        formatBytes(result.downloaded),
                        formatBytes(result.total),
                    ),
                )
            }

            is DownloadResult.Converting -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.status_converting))
            }

            is DownloadResult.Complete -> {
                Text(
                    text = stringResource(R.string.status_complete),
                    color = MaterialTheme.colorScheme.primary,
                )
                SelectionContainer {
                    Text(
                        text = result.file,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { openVideo(context, result.file) },
                    )
                }
            }

            is DownloadResult.Error -> {
                SelectionContainer {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }
    }
}

private fun openVideo(
    context: Context,
    file: String,
) {
    val uri =
        if (file.startsWith("content://")) {
            Uri.parse(file)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(file),
            )
        }

    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.error_no_video_player, Toast.LENGTH_SHORT).show()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
