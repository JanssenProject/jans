package com.example.fido2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.fido2.ui.screens.NavigationRoutes
import org.koin.compose.koinInject
import com.example.fido2.ui.screens.authenticatedGraph
import com.example.fido2.ui.screens.unauthenticatedGraph
import com.example.fido2.viewmodel.MainViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun App() {

    val viewModel: MainViewModel = koinInject()

    MaterialTheme(
        typography = MaterialTheme.typography.copy(
            body1 = MaterialTheme.typography.body1.copy(
                color = Color.White
            )
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                val shouldShowDialog = remember { mutableStateOf(false) }
                val dialogContent = remember { mutableStateOf("") }
                LaunchedEffect(true) {
                    viewModel.loadAppTasks(shouldShowDialog, dialogContent)
                }
                AppAlertDialog(
                    shouldShowDialog = shouldShowDialog,
                    content = dialogContent
                )
                if (!viewModel.mainState.isLoading) {
                    MainApp(viewModel)
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
                        Text(stringResource(Res.string.loading), color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        MainAppNavHost(viewModel = viewModel)
    }
}

@Composable
fun MainAppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavigationRoutes.Unauthenticated.NavigationRoute.route
    ) {
        // Authenticated user flow screens
        authenticatedGraph(navController = navController, viewModel = viewModel)

        // Unauthenticated user flow screens
        unauthenticatedGraph(navController = navController, viewModel = viewModel)
    }

}

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
            .height(120.dp)
            .padding(15.dp)
    ) {
        Row {  // 1
            Text(
                text = "$label:",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                modifier = Modifier
                    .padding(5.dp),
            )
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
    icon: DrawableResource,
    onButtonClick: () -> Unit,
) {
    Card(
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
                        text = stringResource(Res.string.continue_),
                        onClick = onButtonClick
                    )
                }
            }
        }
    }
}
