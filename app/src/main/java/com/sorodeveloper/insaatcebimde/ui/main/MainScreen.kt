package com.sorodeveloper.insaatcebimde.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sorodeveloper.insaatcebimde.ui.invitation.InvitationsScreen
import com.sorodeveloper.insaatcebimde.ui.profile.ProfileScreen
import com.sorodeveloper.insaatcebimde.ui.project.CreateProjectScreen
import com.sorodeveloper.insaatcebimde.ui.project.ProjectDetailScreen

@Composable
fun MainScreen(onLogoutSuccess: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Sadece dashboard, invitations ve profile ekranlarında alt menüyü göster
    val showBottomBar = currentRoute in listOf("dashboard", "invitations", "profile")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Ana Menü") },
                        label = { Text("Ana Menü") },
                        selected = currentRoute == "dashboard",
                        onClick = {
                            if (currentRoute != "dashboard") {
                                navController.navigate("dashboard") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Notifications, contentDescription = "Davetiyelerim") },
                        label = { Text("Davetler") },
                        selected = currentRoute == "invitations",
                        onClick = {
                            if (currentRoute != "invitations") {
                                navController.navigate("invitations") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        selected = currentRoute == "profile",
                        onClick = {
                            if (currentRoute != "profile") {
                                navController.navigate("profile") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") { 
                DashboardScreen(
                    onNavigateToCreateProject = { navController.navigate("create_project") },
                    onNavigateToProjectDetail = { projectId -> 
                        navController.navigate("project_detail/$projectId")
                    }
                ) 
            }
            composable("profile") { ProfileScreen(onLogoutSuccess = onLogoutSuccess) }
            composable("invitations") { InvitationsScreen() }
            composable("create_project") {
                CreateProjectScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onProjectCreated = { 
                        // Kayıt başarılı olunca dashboard'a dön
                        navController.popBackStack("dashboard", inclusive = false) 
                    }
                )
            }
            composable("project_detail/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                ProjectDetailScreen(
                    projectId = projectId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
