package com.example.androidapp.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

@Composable
fun JsonTreeView(jsonString: String) {
    val jsonElement = remember { JsonParser.parseString(jsonString) }
    Column(modifier = Modifier.padding(16.dp)) {
        RenderJsonElement(jsonElement)
    }
}

@Composable
fun RenderJsonElement(element: JsonElement, indent: Int = 0) {
    when {
        element.isJsonObject -> {
            JsonObjectView(element.asJsonObject, indent)
        }
        element.isJsonArray -> {
            JsonArrayView(element.asJsonArray, indent)
        }
        else -> {
            Text(
                text = element.toString(),
                modifier = Modifier.padding(start = indent.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun JsonObjectView(jsonObject: JsonObject, indent: Int) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(start = indent.dp)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(4.dp)) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "{...}", style = MaterialTheme.typography.bodyLarge)
        }
        if (expanded) {
            jsonObject.entrySet().forEach { (key, value) ->
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = "$key: ", style = MaterialTheme.typography.bodyMedium)
                    RenderJsonElement(value, indent + 16)
                }
            }
        }
    }
}

@Composable
fun JsonArrayView(jsonArray: com.google.gson.JsonArray, indent: Int) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(start = indent.dp)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(4.dp)) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "[...]", style = MaterialTheme.typography.bodyLarge)
        }
        if (expanded) {
            jsonArray.forEach { element ->
                RenderJsonElement(element, indent + 16)
            }
        }
    }
}