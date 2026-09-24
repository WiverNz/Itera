package com.itera.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.LocalTime
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    title: String,
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val is24Hour = remember(configuration) {
        android.text.format.DateFormat.is24HourFormat(context)
    }
    val picker = rememberTimePickerState(initialTime.hour, initialTime.minute, is24Hour)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        scrimColor = c.scrim,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = Itera.type.title, color = c.ink)
            TimePicker(state = picker, colors = iteraTimePickerColors())
            IteraButton(stringResource(R.string.action_done), {
                onConfirm(LocalTime.of(picker.hour, picker.minute))
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun iteraTimePickerColors(): androidx.compose.material3.TimePickerColors {
    val c = Itera.colors
    return TimePickerDefaults.colors(
        clockDialColor = c.surface2,
        selectorColor = c.ink,
        containerColor = c.surface,
        clockDialSelectedContentColor = c.onInk,
        clockDialUnselectedContentColor = c.ink,
        periodSelectorBorderColor = c.line,
        periodSelectorSelectedContainerColor = c.ink,
        periodSelectorUnselectedContainerColor = c.surface2,
        periodSelectorSelectedContentColor = c.onInk,
        periodSelectorUnselectedContentColor = c.ink,
        timeSelectorSelectedContainerColor = c.ink,
        timeSelectorUnselectedContainerColor = c.surface2,
        timeSelectorSelectedContentColor = c.onInk,
        timeSelectorUnselectedContentColor = c.ink
    )
}
