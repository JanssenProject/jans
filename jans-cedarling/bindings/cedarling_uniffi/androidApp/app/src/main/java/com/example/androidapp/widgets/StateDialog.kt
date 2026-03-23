package com.example.androidapp.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidapp.utils.toJsonString
import com.example.androidapp.views.JsonTreeView
import uniffi.cedarling_uniffi.MultiIssuerAuthorizeResult


/**
 * Displays a dialog showing the current authorization state, optional diagnostics, and optional logs.
 *
 * The dialog presents the decision from `result`, renders `result.response.diagnostics` as JSON when present,
 * and lists any provided `logs`. Dismissing the dialog or tapping the Close button invokes `onDismiss`.
 *
 * @param result The authorization result to display; may be `null`.
 * @param logs Optional list of log entries to display; may be `null`.
 * @param onDismiss Callback invoked when the dialog is dismissed or the Close button is pressed.
 */
@Composable
fun StateDialog(
    result: MultiIssuerAuthorizeResult?,
    logs: List<String>?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Current State") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()) // Make content scrollable
            ) {
                Text("Authorization Result:", style = MaterialTheme.typography.titleMedium)
                Text("${result?.decision ?: "N/A"}", modifier = Modifier.padding(4.dp))

                result?.response?.diagnostics?.let { diagnostics ->
                    Text("Diagnostics:", style = MaterialTheme.typography.titleMedium)
                    JsonTreeView(toJsonString(diagnostics))
                }
                Text("Logs:", style = MaterialTheme.typography.titleMedium)
                logs?.forEach { log ->
                    JsonTreeView(log)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}