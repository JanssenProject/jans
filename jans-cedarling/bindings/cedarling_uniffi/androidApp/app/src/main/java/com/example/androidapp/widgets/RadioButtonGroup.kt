package com.example.androidapp.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadioButtonGroup(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val options = listOf("Decision", "System", "Metric")
    //var selectedOption by remember { mutableStateOf(options[0]) } // Default selection

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select an LogType:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    //.clickable { selectedOption = option } // Click anywhere to select
                    .padding(8.dp)
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) }
                )
                Text(text = option, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}