package io.jans.chip

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import com.nimbusds.jwt.JWTClaimsSet
import com.spr.jetpack_loading.components.indicators.lineScaleIndicator.LineScaleIndicator
import com.spr.jetpack_loading.enums.PunchType
import io.jans.chip.factories.DPoPProofFactory
import io.jans.chip.model.OIDCClient
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.UserInfoResponse
import io.jans.chip.model.appIntegrity.AppIntegrityResponse
import io.jans.chip.ui.screens.NavigationRoutes
import io.jans.chip.ui.screens.authenticatedGraph
import io.jans.chip.ui.screens.unauthenticatedGraph
import io.jans.chip.utils.AppConfig
import io.jans.chip.viewmodel.MainViewModel
import io.jans.jans_chip.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //mainViewModel.initModel(this)
        val mainViewModel = MainViewModel.getInstance(this)
        var loading = true

        CoroutineScope(Dispatchers.IO).launch {

            mainViewModel.opConfigurationPresent = false
            mainViewModel.fidoConfigurationPresent = false
            mainViewModel.attestationOptionSuccess = false
            mainViewModel.attestationOptionResponse = false
            mainViewModel.clientRegistered = false
            mainViewModel.userIsAuthenticated = false

            mainViewModel.errorInLoading = false

            //get openid configuration
            try {
                var opConfiguration: OPConfiguration? =
                    async { mainViewModel.getOPConfigurationInDatabase() }.await()
                if (opConfiguration != null) {
                    mainViewModel.opConfigurationPresent = true
                }
                if (opConfiguration == null) {
                    val jwtClaimsSet: JWTClaimsSet = DPoPProofFactory.getClaimsFromSSA()
                    val issuer: String = jwtClaimsSet.getClaim("iss").toString()
                    mainViewModel.setOpConfigUrl(issuer + AppConfig.OP_CONFIG_URL)
                    opConfiguration = async { mainViewModel.fetchOPConfiguration() }.await()
                    if (opConfiguration == null || opConfiguration.isSuccessful == false) {
                        mainViewModel.errorInLoading = true
                        mainViewModel.loadingErrorMessage = "Error in fetching OP Configuration"
                        throw Exception("Error in fetching OP Configuration")
                    }
                    mainViewModel.opConfigurationPresent = true
                }

                //get FIDO configuration
                val fidoConfiguration = async { mainViewModel.getFidoConfigInDatabase() }.await()
                if (fidoConfiguration != null) {
                    mainViewModel.fidoConfigurationPresent = true
                }
                if (fidoConfiguration == null) {
                    val jwtClaimsSet: JWTClaimsSet = DPoPProofFactory.getClaimsFromSSA()
                    val issuer: String = jwtClaimsSet.getClaim("iss").toString()
                    mainViewModel.setFidoConfigUrl(issuer + AppConfig.FIDO_CONFIG_URL)
                    val fidoConfigurationResponse =
                        async { mainViewModel.fetchFidoConfiguration() }.await()

                    if (fidoConfigurationResponse == null || fidoConfigurationResponse.isSuccessful == false) {
                        mainViewModel.errorInLoading = true
                        mainViewModel.loadingErrorMessage = "Error in fetching FIDO Configuration"
                        throw Exception("Error in fetching FIDO Configuration")
                    }
                    mainViewModel.fidoConfigurationPresent = true
                }
                //check OIDC client
                var oidcClient: OIDCClient? = async { mainViewModel.getClientInDatabase() }.await()

                if (oidcClient != null) {
                    mainViewModel.clientRegistered = true
                }
                if (oidcClient == null) {
                    oidcClient = mainViewModel.doDCRUsingSSA(
                        AppConfig.SSA,
                        AppConfig.ALLOWED_REGISTRATION_SCOPES
                    )
                    mainViewModel.clientRegistered = oidcClient != null
                    if (oidcClient == null || oidcClient.isSuccessful == false) {
                        mainViewModel.errorInLoading = true
                        mainViewModel.loadingErrorMessage = "Error in registering OIDC Client"
                        throw Exception("Error in registering OIDC Client")
                    }
                }
                val userInfoResponse: UserInfoResponse? =
                    mainViewModel.getUserInfoWithAccessToken(oidcClient.recentGeneratedAccessToken)
                if (userInfoResponse?.isSuccessful == true) {
                    mainViewModel.setUserInfoResponse(userInfoResponse)
                    mainViewModel.userIsAuthenticated = true
                }


                var appIntegrityEntity: String? =
                    async { mainViewModel.checkAppIntegrityFromDatabase() }.await()
                if (appIntegrityEntity == null) {
                    val appIntegrityResponse: AppIntegrityResponse? =
                        async { mainViewModel.checkAppIntegrity() }.await()
                    if (appIntegrityResponse != null) {
                        mainViewModel.errorInLoading = true
                        mainViewModel.loadingErrorMessage =
                            appIntegrityResponse.appIntegrity?.appRecognitionVerdict
                                ?: "Unable to fetch App Integrity from Google Play Integrity"
                    }
                } else {
                    mainViewModel.errorInLoading = true
                    mainViewModel.loadingErrorMessage = "App Integrity: ${appIntegrityEntity}"
                }
                loading = false
            } catch (e: Exception) {
                //catching exception
                loading = false
                mainViewModel.errorInLoading = true
                mainViewModel.loadingErrorMessage = "Error in loading app: ${e.message}"
                e.printStackTrace()
            }

        }
        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loadingApp = remember { mutableStateOf(loading) }
                    val shouldShowDialog = remember { mutableStateOf(false) }
                    val dialogContent = remember { mutableStateOf("") }

                    shouldShowDialog.value = mainViewModel.errorInLoading
                    dialogContent.value = mainViewModel.loadingErrorMessage
                    AppAlertDialog(
                        shouldShowDialog = shouldShowDialog,
                        content = dialogContent
                    )
                    if (!loading) {
                        MainApp()
                    } else {
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
            }
        }
    }
}

@Composable
fun MainApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MainAppNavHost()
    }

}

@Composable
fun MainAppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavigationRoutes.Unauthenticated.NavigationRoute.route
    ) {
        // Authenticated user flow screens
        authenticatedGraph(navController = navController)

        // Unauthenticated user flow screens
        unauthenticatedGraph(navController = navController)
    }

}

@Composable
fun AppAlertDialog(shouldShowDialog: MutableState<Boolean>, content: MutableState<String>) {
    if (shouldShowDialog.value) {
        AlertDialog(
            onDismissRequest = {
                shouldShowDialog.value = false
            },

            title = { Text(text = "Warning") },
            text = { Text(text = content.value) },
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
fun AppLoaderDialog(shouldShowDialog: MutableState<Boolean>) {
    if (shouldShowDialog.value) {
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

@Composable
fun Title(
    // 1
    text: String,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
) {
    Text(  // 2
        text = text,
        style = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,  // 3
        )
    )
}

@Composable
fun LogButton(
    // 1
    text: String,
    isClickable: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
        Button(
            // 3
            enabled = isClickable,
            onClick = { onClick() },
            modifier = Modifier
                .width(200.dp)
                .height(50.dp),
        ) {
            Text(
                // 4
                text = text,
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
fun UserInfoRow(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        )
    ) {
        Row {  // 1
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                modifier = Modifier
                    .padding(5.dp),
            )
        }
        Divider(color = Color.Gray, thickness = 1.dp)
        Row {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontSize = 20.sp,

                    ),
                modifier = Modifier
                    .padding(5.dp),
            )
        }
    }
}

@Composable
fun ElevatedCardExample(
    heading: String,
    subheading: String,
    @DrawableRes icon: Int,
    onButtonClick: () -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
    ) {

        Row {  // 1
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .height(150.dp)
            ) {
                Image(
                    painterResource(icon),
                    contentDescription = "",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column {
                Row {
                    Text(
                        text = heading,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        modifier = Modifier
                            .padding(1.dp),
                    )
                }
                Row {
                    Text(
                        text = subheading,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                        ),
                        modifier = Modifier
                            .padding(1.dp),
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    LogButton(
                        isClickable = true,
                        text = stringResource(R.string.continue_),
                        onClick = onButtonClick,
                    )
                }
            }
        }
    }
}