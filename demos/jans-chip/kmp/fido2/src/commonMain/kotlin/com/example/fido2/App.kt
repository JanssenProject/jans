package com.example.fido2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fido2.ui.common.customComposableViews.CustomAlertDialog
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
                CustomAlertDialog(
                    stringResource(Res.string.warning),
                    dialogContent.value,
                    stringResource(Res.string.ok),
                    shouldShowDialog
                ) {
                    // Action
                }
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
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel
) {
    val screens = listOf(
        NavigationRoutes.Unauthenticated.NavigationRoute.route,
        NavigationRoutes.Unauthenticated.Settings.route
    )
    Scaffold(bottomBar = {
        BottomNavigation(modifier =  Modifier.height(60.dp), backgroundColor = Color.White,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                        shadowElevation = 2.2f
                    }
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    BottomNavigationItem(icon = {
                        Icon(
                            painter = painterResource(getIconForScreen(screen)),
                            contentDescription = null
                        )
                    },
                        selected = currentDestination?.hierarchy?.any { it.route == screen } == true,
                        onClick = {
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().route.toString()) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                }
            }
        }
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.Unauthenticated.NavigationRoute.route,
            Modifier.padding(innerPadding)
        ) {
            // Authenticated user flow screens
            authenticatedGraph(navController = navController, viewModel = viewModel)

            // Unauthenticated user flow screens
            unauthenticatedGraph(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
fun getIconForScreen(screen: String): DrawableResource {
    return when (screen) {
        NavigationRoutes.Unauthenticated.NavigationRoute.route -> Res.drawable.home_icon
        NavigationRoutes.Unauthenticated.Settings.route -> Res.drawable.settings_icon
        else -> Res.drawable.home_icon
    }
}
