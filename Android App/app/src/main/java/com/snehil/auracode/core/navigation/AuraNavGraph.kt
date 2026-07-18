package com.snehil.auracode.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snehil.auracode.mainui.auth.login.LoginScreen
import com.snehil.auracode.mainui.auth.signup.SignupScreen
import com.snehil.auracode.mainui.billing.BillingScreen
import com.snehil.auracode.mainui.dashboard.DashboardScreen
import com.snehil.auracode.mainui.splashscreen.ui.SplashScreen
import com.snehil.auracode.mainui.workspace.WorkspaceScreen

@Composable
fun AuraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val rootViewModel: RootViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        rootViewModel.forceLogout.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onAuthenticated = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onUnauthenticated = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Routes.SIGNUP)
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignedUp = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenProject = { projectId ->
                    navController.navigate(Routes.workspace(projectId))
                },
                onOpenBilling = {
                    navController.navigate(Routes.BILLING)
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BILLING) {
            BillingScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.WORKSPACE,
            arguments = listOf(navArgument(Routes.ARG_PROJECT_ID) { type = NavType.LongType })
        ) { entry ->
            val projectId = entry.arguments?.getLong(Routes.ARG_PROJECT_ID) ?: 0L
            WorkspaceScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
