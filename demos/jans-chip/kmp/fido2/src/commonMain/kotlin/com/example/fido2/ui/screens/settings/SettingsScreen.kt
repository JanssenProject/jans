package com.example.fido2.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.fido2.Res
import com.example.fido2.enter_url
import com.example.fido2.loading
import com.example.fido2.save
import com.example.fido2.ui.common.customComposableViews.CustomTextField
import com.example.fido2.ui.common.customComposableViews.LoginButton
import com.example.fido2.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = koinInject()
) {
    viewModel.settingsState = viewModel.settingsState.copy(isLoading = true)

    var text by remember {
        mutableStateOf(TextFieldValue(viewModel.settingsState.serverUrl ?: ""))
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(true) {
        text = TextFieldValue(viewModel.getServerUrl())
        viewModel.settingsState = viewModel.settingsState.copy(serverUrl = viewModel.getServerUrl(), isLoading = false)
    }

    val onServerUrlChange: (String) -> Unit = { url ->
        text = TextFieldValue(url)
        viewModel.settingsState.serverUrl = text.text
    }
    val onButtonClick: () -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.saveServerUrl()
            keyboardController?.hide()
        }
    }

    if (viewModel.settingsState.serverUrl == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .height(400.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(stringResource(Res.string.loading), color = Color.Black)
        }
    } else {
        Column(
            modifier = Modifier.background(Color.White).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            CustomTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimens.paddingLarge),
                value = text.text,
                onValueChange = onServerUrlChange,
                label = stringResource(Res.string.enter_url),
                isError = viewModel.settingsState.errorState.urlErrorState.hasError,
                errorText = stringResource(viewModel.settingsState.errorState.urlErrorState.errorMessageStringResource),
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.End) {
                LoginButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(Res.string.save),
                    onClick = onButtonClick
                )
            }
        }
    }
}