package io.jans.chip.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spr.jetpack_loading.components.indicators.lineScaleIndicator.LineScaleIndicator
import com.spr.jetpack_loading.enums.PunchType
import io.jans.chip.AppAlertDialog
import io.jans.chip.LogButton
import io.jans.chip.UserInfoRow
import io.jans.chip.model.LogoutResponse
import io.jans.chip.ui.common.customComposableViews.TitleText
import io.jans.chip.ui.screens.unauthenticated.login.LoginViewModel
import io.jans.chip.ui.screens.unauthenticated.login.state.LoginUiEvent
import io.jans.chip.viewmodel.MainViewModel
import io.jans.jans_chip.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun DashboardScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onNavigateToUnAuthenticatedRoute: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity
    var loading by remember { mutableStateOf(false) }
    val shouldShowDialog = remember { mutableStateOf(false) }
    val dialogContent = remember { mutableStateOf("") }
    val loginState by remember {
        loginViewModel.loginState
    }
    val mainViewModel = MainViewModel.getInstance(context)
    AppAlertDialog(
        shouldShowDialog = shouldShowDialog,
        content = dialogContent
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        if (mainViewModel.mainState.isClientRegistered && (mainViewModel.mainState.attestationResultSuccess || mainViewModel.mainState.assertionResultSuccess)) {
            if (mainViewModel.mainState.isUserIsAuthenticated) {
                TitleText(text = stringResource(id = R.string.dashboard_title_welcome) + " " + mainViewModel.getUsername())
                if (mainViewModel.getUserInfoResponse().response != null) {
                    val userInfo = JSONObject(mainViewModel.getUserInfoResponse().response.toString())
                    val keys = userInfo.keys()
                    while (keys.hasNext()) {
                        val key: String = keys.next()
                        UserInfoRow(key, userInfo.get(key).toString())
                    }
                }
                LogButton(
                    isClickable = true,
                    text = stringResource(R.string.logout),
                    onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            loading = true
                            val logoutResponse: LogoutResponse = async { mainViewModel.logout() }.await()
                            if (logoutResponse.isSuccessful != true) {
                                shouldShowDialog.value = true
                                dialogContent.value = logoutResponse.errorMessage.toString()
                            }
                            loginViewModel.onUiEvent(loginUiEvent = LoginUiEvent.Logout)
                            onNavigateToUnAuthenticatedRoute.invoke()
                            loading = false
                        }
                    },
                )
            }
        }
        if (loading) {
            Spacer(modifier = Modifier.height(40.dp))
            LineScaleIndicator(
                color = Color(0xFF134520),
                rectCount = 5,
                distanceOnXAxis = 30f,
                lineHeight = 100,
                animationDuration = 500,
                minScale = 0.3f,
                maxScale = 1.5f,
                punchType = PunchType.RANDOM_PUNCH,
                penThickness = 15f
            )
        }
    }
}
