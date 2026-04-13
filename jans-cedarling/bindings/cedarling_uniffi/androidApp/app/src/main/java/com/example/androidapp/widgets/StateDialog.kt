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