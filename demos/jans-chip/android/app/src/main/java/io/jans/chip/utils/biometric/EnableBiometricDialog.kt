package io.jans.chip.utils.biometric

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.jans.jans_chip.R

@Composable
fun EnableBiometricDialog(
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { },
        text = {
            Text(text = stringResource(R.string.enable_biometric_dialog_title_text))
        },
        confirmButton = {
            Button(onClick = {
                onEnable()
            }) {
                Text(text = stringResource(R.string.enable_biometric_dialog_confirm_btn_text))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.enable_biometric_dialog_dismiss_btn_text))
            }
        }
    )
}