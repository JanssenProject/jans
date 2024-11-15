package com.example.fido2.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fido2.*
import com.example.fido2.ui.common.customComposableViews.TitleText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.example.fido2.model.LogoutResponse
import com.example.fido2.model.UserDetails
import com.example.fido2.ui.common.customComposableViews.AppAlertDialog
import com.example.fido2.ui.common.customComposableViews.LogButton
import com.example.fido2.ui.common.customComposableViews.UserInfoRow
import com.example.fido2.ui.screens.unauthenticated.login.LoginViewModel
import com.example.fido2.ui.screens.unauthenticated.login.state.LoginUiEvent
import com.example.fido2.viewmodel.MainViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(
    loginViewModel: LoginViewModel = viewModel { LoginViewModel() },
    viewModel: MainViewModel,
    onNavigateToUnAuthenticatedRoute: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    val shouldShowDialog = remember { mutableStateOf(false) }
    val dialogContent = remember { mutableStateOf("") }

    AppAlertDialog(
        shouldShowDialog = shouldShowDialog,
        content = dialogContent
    )
    // Full Screen Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewModel.mainState.attestationResultSuccess || viewModel.mainState.assertionResultSuccess) { // viewModel.mainState.isClientRegistered && (
            if (viewModel.mainState.isUserIsAuthenticated) {
                TitleText(text = "Welcome" + " " + viewModel.getUsername())
                if (viewModel.getUserInfoResponse()?.response != null) {
                    val userInfo: UserDetails = Json.decodeFromString(viewModel.getUserInfoResponse()?.response.toString())
                    for (user in userInfo.info()) {
                        UserInfoRow(user.key, user.value)
                    }
                }
                LogButton(
                    isClickable = true,
                    text = stringResource(Res.string.logout),
                    onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            loading = true
                            val logoutResponse: LogoutResponse? = async { viewModel.logout() }.await()
                            if (logoutResponse?.isSuccessful != true) {
                                shouldShowDialog.value = true
                                dialogContent.value = logoutResponse?.errorMessage.toString()
                            } else {
                                viewModel.isRegistrationSuccessful.value = false
                                viewModel.isLogoutSuccessful.value = true
                                loginViewModel.onUiEvent(loginUiEvent = LoginUiEvent.Logout)
                                onNavigateToUnAuthenticatedRoute.invoke()

                                loading = false
                            }
                        }
                    }
                )
            }
        }
        if (loading) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("Loading", color = Color.Black)
        }
    }
}
