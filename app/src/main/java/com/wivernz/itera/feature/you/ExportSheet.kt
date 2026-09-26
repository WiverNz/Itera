package com.wivernz.itera.feature.you

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.Segmented
import com.wivernz.itera.analytics.ExportRange
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.domain.repository.JournalExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportDestination { SHARE, SAVE }
data class ExportUiState(
    val range: ExportRange = ExportRange.MONTH,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val noTarget: Boolean = false,
    val saved: Boolean = false
)
data class ExportEffect(val file: File, val destination: ExportDestination)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exporter: JournalExporter,
    private val savedState: SavedStateHandle,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val mutable = MutableStateFlow(ExportUiState())
    val state = mutable.asStateFlow()
    private val channel = Channel<ExportEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    fun range(range: ExportRange) {
        mutable.update { it.copy(range = range) }
    }
    fun export(destination: ExportDestination, locale: Locale) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, failed = false, noTarget = false, saved = false) }
        viewModelScope.launch {
            try {
                val file = exporter.export(state.value.range, locale)
                savedState["file"] = file.absolutePath
                channel.send(ExportEffect(file, destination))
            } catch (
                e: CancellationException
            ) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                mutable.update { it.copy(failed = true) }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }
    fun noTarget() {
        mutable.update { it.copy(noTarget = true) }
        finished()
    }
    fun finished() {
        val path = savedState.get<String>("file") ?: return
        savedState["file"] = null
        viewModelScope.launch { exporter.cleanup(File(path)) }
    }
    fun save(context: Context, uri: Uri?) {
        if (uri == null) {
            finished()
            return
        }
        val path = savedState.get<String>("file") ?: return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(io) {
                    checkNotNull(context.contentResolver.openOutputStream(uri)).use { out ->
                        File(path).inputStream().use { it.copyTo(out) }
                    }
                }
                mutable.update { it.copy(saved = true) }
            } catch (
                e: CancellationException
            ) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                mutable.update { it.copy(failed = true) }
            } finally {
                mutable.update { it.copy(busy = false) }
                finished()
            }
        }
    }
}

fun journalShareIntent(context: Context, file: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return journalShareIntent(uri)
}

fun journalShareIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_SEND).setType("text/markdown").putExtra(Intent.EXTRA_STREAM, uri)
        .apply {
            clipData = ClipData.newRawUri(null, uri)
        }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

@Composable
fun ExportRoute(vm: ExportViewModel, dismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = currentLocale()
    val state by vm.state.collectAsStateWithLifecycle()
    val share =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            vm.finished()
        }
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) {
            vm.save(context, it)
        }
    ObserveEffects(vm.effects) { effect ->
        try {
            if (effect.destination == ExportDestination.SAVE) {
                save.launch(effect.file.name)
            } else {
                val intent = journalShareIntent(context, effect.file)
                if (context.packageManager.queryIntentActivities(intent, 0).isEmpty()) {
                    vm.noTarget()
                } else {
                    share.launch(
                        Intent.createChooser(intent, context.getString(R.string.export_journal))
                    )
                }
            }
        } catch (_: ActivityNotFoundException) {
            vm.noTarget()
        }
    }
    ExportSheet(state, vm::range, { vm.export(it, locale) }, dismiss)
}

@Composable
fun ExportSheet(
    state: ExportUiState,
    range: (ExportRange) -> Unit,
    destination: (ExportDestination) -> Unit,
    dismiss: () -> Unit
) {
    SettingsSheet(stringResource(R.string.export_journal), {
        if (!state.busy) dismiss()
    }, done = false) {
        Text(
            stringResource(R.string.export_note),
            style = Itera.type.bodySmall,
            color = Itera.colors.ink2
        )
        Segmented(
            listOf(
                ExportRange.MONTH to stringResource(R.string.export_month),
                ExportRange.YEAR to stringResource(R.string.export_year),
                ExportRange.ALL to stringResource(R.string.export_all)
            ),
            state.range,
            range
        )
        if (state.failed) {
            Text(
                stringResource(R.string.error_export),
                style = Itera.type.bodySmall,
                color = Itera.colors.ink2
            )
        }
        if (state.noTarget) {
            Text(
                stringResource(R.string.export_no_target),
                style = Itera.type.bodySmall,
                color = Itera.colors.ink2
            )
        }
        if (state.saved) {
            Text(
                stringResource(R.string.export_saved),
                style = Itera.type.bodySmall,
                color = Itera.colors.ink2
            )
        }
        IteraButton(stringResource(R.string.export_share), {
            destination(ExportDestination.SHARE)
        }, enabled = !state.busy)
        IteraButton(stringResource(R.string.export_save), {
            destination(ExportDestination.SAVE)
        }, kind = ButtonKind.Secondary, enabled = !state.busy)
    }
}
