package com.example.fido2.ui.screens

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.fido2.ui.screens.dashboard.DashboardScreen
import com.example.fido2.ui.screens.settings.SettingsScreen
import com.example.fido2.ui.screens.unauthenticated.login.LoginScreen
import com.example.fido2.ui.screens.unauthenticated.registration.RegistrationScreen
import com.example.fido2.viewmodel.MainViewModel

/**
 * Login, registration, forgot password screens nav graph builder
 * (Unauthenticated user)
 */

fun NavGraphBuilder.unauthenticatedGraph(
    viewModel: MainViewModel,
    navController: NavController
) {

    navigation(
        route = NavigationRoutes.Unauthenticated.NavigationRoute.route,
        startDestination = NavigationRoutes.Unauthenticated.Registration.route
    ) {

        // Login
        composable(route = NavigationRoutes.Unauthenticated.Login.route) {
            LoginScreen(
                onNavigateToRegistration = {
                    navController.navigate(route = NavigationRoutes.Unauthenticated.Registration.route)
                },
                onNavigateToAuthenticatedRoute = {
                    navController.navigate(route = NavigationRoutes.Authenticated.NavigationRoute.route) {
                        popUpTo(route = NavigationRoutes.Unauthenticated.NavigationRoute.route) {
                            inclusive = true
                        }
                    }
                },
                viewModel = viewModel
            )
        }
        // Registration
        composable(route = NavigationRoutes.Unauthenticated.Registration.route) {
            RegistrationScreen(
                onNavigateBack = {
                    //navController.navigateUp()
                    navController.navigate(route = NavigationRoutes.Unauthenticated.Login.route)
                },
                onNavigateToAuthenticatedRoute = {
                    navController.navigate(route = NavigationRoutes.Authenticated.NavigationRoute.route) {
                        popUpTo(route = NavigationRoutes.Unauthenticated.NavigationRoute.route) {
                            inclusive = true
                        }
                    }
                },
                viewModel = viewModel,
                onQRCodeScanClose = {
                    navController.navigate(route = NavigationRoutes.Unauthenticated.Registration.route)
                }
            )
        }
        // Settings
        composable(route = NavigationRoutes.Unauthenticated.Settings.route) {
            SettingsScreen()
        }
    }
}

/**
 * Authenticated screens nav graph builder
 */
fun NavGraphBuilder.authenticatedGraph(
    viewModel: MainViewModel,
    navController: NavController
) {
    navigation(
        route = NavigationRoutes.Authenticated.NavigationRoute.route,
        startDestination = NavigationRoutes.Authenticated.Dashboard.route
    ) {
        // Dashboard
        composable(route = NavigationRoutes.Authenticated.Dashboard.route) {
            DashboardScreen(
                onNavigateToUnAuthenticatedRoute = {
                    navController.navigate(route = NavigationRoutes.Unauthenticated.NavigationRoute.route) {
                        popUpTo(route = NavigationRoutes.Authenticated.NavigationRoute.route) {
                            inclusive = true
                        }
                    }
                },
                viewModel = viewModel
            )
        }
    }
}
