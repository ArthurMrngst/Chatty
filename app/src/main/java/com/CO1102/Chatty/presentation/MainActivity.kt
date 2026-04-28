package com.CO1102.Chatty.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.CO1102.Chatty.domain.model.Group
import com.CO1102.Chatty.presentation.screens.chat.ChatListScreen
import com.CO1102.Chatty.presentation.screens.auth.LoginScreen
import com.CO1102.Chatty.presentation.screens.auth.RegisterScreen
import com.CO1102.Chatty.presentation.screens.chat.GroupChatScreen
import com.CO1102.Chatty.presentation.viewmodel.AuthViewModel
import com.CO1102.Chatty.ui.theme.ChattyTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.CO1102.Chatty.presentation.screens.MeScreen
import com.CO1102.Chatty.presentation.screens.PeopleScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChattyTheme {
                ChattyApp()
            }
        }
    }
}

@Composable
fun ChattyApp() {
    val navController  = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // Shared group state passed between ChatList → GroupChat
    var selectedGroup by remember { mutableStateOf<Group?>(null) }

    // Decide start destination — skip login if already signed in
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "chat_list" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Login ─────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    authViewModel.resetState()
                    navController.navigate("chat_list") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoRegister = {
                    navController.navigate("register")
                },
                viewModel = authViewModel
            )
        }

        // ── Register ──────────────────────────────────────────────────────
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    authViewModel.resetState()
                    navController.navigate("chat_list") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoLogin = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        // ── Chat List ─────────────────────────────────────────────────────
        composable("chat_list") {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            ChatListScreen(
                currentUserId  = currentUserId,
                onGroupClick   = { group ->
                    selectedGroup = group
                    navController.navigate("group_chat")
                },
                onNewChatClick = { },
                onPeopleClick  = { navController.navigate("people") },
                onMeClick      = { navController.navigate("me") }
            )
        }
        // ── Group Chat ────────────────────────────────────────────────────
        composable("group_chat") {
            val group = selectedGroup
            if (group != null) {
                GroupChatScreen(
                    group       = group,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        // ── HOME / PEOPLE ──────────────────────────────────────────────────────
        composable("people") {
            PeopleScreen(
                onOpenChat = { group ->
                    selectedGroup = group
                    navController.navigate("group_chat")
                },
                onBackToChats = {
                    navController.navigate("chat_list") {
                        popUpTo("chat_list") { inclusive = false }
                    }
                },
                onMeClick = { navController.navigate("me") }
            )
        }
        composable("me") {
            MeScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBackToChats = {
                    navController.navigate("chat_list") {
                        popUpTo("chat_list") { inclusive = false }
                    }
                }
            )
        }
    }
}