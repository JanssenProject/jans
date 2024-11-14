package com.example.fido2.ui.common.customComposableViews

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import com.example.fido2.Res
import com.example.fido2.cancel
import com.example.fido2.delete_all
import com.example.fido2.yes
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppAlertDialog(shouldShowDialog: MutableState<Boolean>, content: MutableState<String>) {
    if (shouldShowDialog.value) {
        AlertDialog(
            onDismissRequest = {
                shouldShowDialog.value = false
            },

            title = { Text(text = "Warning", color = Color.Red) },
            text = { Text(text = content.value, color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                    }
                ) {
                    Text(
                        text = "Ok",
                        color = Color.White
                    )
                }
            }
        )
    }
}

@Composable
fun AppAlertDialogWithTwoButtons(shouldShowDialog: MutableState<Boolean>, text: String, callback: () -> Unit) {
    if (shouldShowDialog.value) {
        AlertDialog(
            onDismissRequest = {
                shouldShowDialog.value = false
            },
            title = { Text(text = stringResource(Res.string.delete_all), color = Color.Black) },
            text = { Text(text = text, color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                        callback.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(Color.Magenta)
                ) {
                    Text(
                        text = stringResource(Res.string.yes),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.cancel),
                        color = Color.White
                    )
                }
            }
        )
    }
}