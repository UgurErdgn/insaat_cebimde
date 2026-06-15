package com.sorodeveloper.insaatcebimde

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.sorodeveloper.insaatcebimde.ui.auth.LoginScreen
import com.sorodeveloper.insaatcebimde.ui.auth.RegisterScreen
import com.sorodeveloper.insaatcebimde.ui.main.MainScreen
import com.sorodeveloper.insaatcebimde.ui.theme.InsaatCebimdeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InsaatCebimdeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val startDest = if (currentUser != null) "main" else "login"

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true } // Login ve Register ekranlarını temizle
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack() // Geri tuşu gibi logine dön
                                }
                            )
                        }
                        composable("main") {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }
}
