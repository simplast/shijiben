package com.doer.shijiben.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.theme.CoralOrange
import com.doer.shijiben.ui.theme.CoralOrangeDark
import com.doer.shijiben.ui.theme.CreamWhite
import com.doer.shijiben.ui.theme.DeepTeal
import com.doer.shijiben.ui.theme.PixelButton
import com.doer.shijiben.ui.theme.PixelCheckIcon
import com.doer.shijiben.ui.theme.PixelCloseIcon
import com.doer.shijiben.ui.theme.PixelDialog
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.SeaBlue
import com.doer.shijiben.ui.theme.SeaBlueDark
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = TimeFormats.localDateToPickerUtcMillis(initialDate),
    )

    PixelDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CreamWhite,
        borderOuterColor = DeepTeal,
        borderInnerColor = Color.White,
    ) {
        DatePicker(state = pickerState)

        Spacer(modifier = Modifier.padding(top = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            PixelButton(
                onClick = onDismiss,
                backgroundColor = Color.Transparent,
                pressedBackgroundColor = CoralOrange.copy(alpha = 0.1f),
                contentColor = CoralOrange,
                borderColor = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                PixelCloseIcon(color = CoralOrange, size = 14.dp)
                Spacer(Modifier.width(4.dp))
                Text("取消", style = PixelLabel, color = CoralOrange)
            }
            Spacer(Modifier.width(8.dp))
            PixelButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onConfirm(TimeFormats.pickerUtcMillisToLocalDate(millis))
                    } else {
                        onDismiss()
                    }
                },
                backgroundColor = SeaBlue,
                pressedBackgroundColor = SeaBlueDark,
                contentColor = Color.White,
                borderColor = DeepTeal,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                PixelCheckIcon(color = Color.White, size = 14.dp)
                Spacer(Modifier.width(4.dp))
                Text("确定", style = PixelLabel, color = Color.White)
            }
        }
    }
}
