package com.example.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.utils.addFieldToJson
import com.example.androidapp.utils.anyToJson
import com.example.androidapp.utils.jsonToMapWithAnyType
import com.example.androidapp.utils.jsonToMapWithStringType
import com.example.androidapp.utils.readJsonFromAssets
import com.example.androidapp.widgets.DataCard
import com.example.androidapp.widgets.RadioButtonGroup
import com.example.androidapp.widgets.StateDialog
import uniffi.mobile.AuthorizeResult
import uniffi.mobile.Cedarling
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CardListScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun CardListScreen() {
    val contextApplication = androidx.compose.ui.platform.LocalContext.current
    var bootstrapConfig by rememberSaveable { mutableStateOf(readJsonFromAssets(contextApplication, "bootstrap.json")) }
    var resource by rememberSaveable { mutableStateOf(readJsonFromAssets(contextApplication, "resource.json")) }
    var context by rememberSaveable { mutableStateOf(readJsonFromAssets(contextApplication, "context.json")) }
    var tokens by rememberSaveable { mutableStateOf(readJsonFromAssets(contextApplication, "tokens.json")) }
    var logType by rememberSaveable { mutableStateOf("Decision") }
    var action by rememberSaveable { mutableStateOf(readJsonFromAssets(contextApplication, "action.txt")) }

    var showDialog by remember { mutableStateOf(false) } // Controls modal visibility


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(25.dp))
        bootstrapConfig?.let {
            DataCard(
                stringResource(R.string.bootstrap_configuration),
                it
            )
        }
        tokens?.let {
            val map: Map<String, Any> = remember(it) {
                Gson().fromJson(it, object : TypeToken<Map<String, Any>>() {}.type)
            }
            map.forEach { (key, value) ->
                DataCard(
                    key,
                    value
                )
            }

        }
        resource?.let {
            DataCard(
                stringResource(R.string.resources),
                it
            )
        }
        context?.let {
            DataCard(
                stringResource(R.string.context),
                it
            )
        }
        RadioButtonGroup(
            selectedOption = logType,
            onOptionSelected = { logType = it }
        )
        Button(onClick = { showDialog = true }) {
            Text("Show State")
        }

        // Display the modal dialog
        if (showDialog) {
            val instance: Cedarling? = bootstrapConfig?.let {
                Cedarling.loadFromJson(it);
            };

            val nullableTokensMap: Map<String, String>? = tokens?.let { jsonToMapWithStringType(it) };
            val tokensMap: Map<String, String> = nullableTokensMap.orEmpty();

            val nonNullableAction: String = action.orEmpty()

            val nullableResource = resource?.let { jsonToMapWithAnyType(it) };
            val resourceMap = nullableResource.orEmpty();

            val nonNullableConext: String = context.orEmpty()

            val result: AuthorizeResult? = instance?.authorize(tokensMap,
                nonNullableAction,
                resourceMap.get("resource_type").toString(),
                resourceMap.get("resource_id").toString(),
                anyToJson(resourceMap.get("payload")),
                nonNullableConext
            );

            val logs: List<String>? = instance?.getLogsByRequestIdAndTag(result?.requestId.orEmpty(), logType)
            StateDialog(
                result = result,
                logs = logs,
                onDismiss = { showDialog = false }
            )
        }
    }
}