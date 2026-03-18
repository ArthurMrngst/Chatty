package com.CO1102.Chatty

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.CO1102.Chatty.ui.theme.ChattyTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        Log.d("FirebaseTest", "Connected: $auth")

        enableEdgeToEdge()

        setContent {
            ChattyTheme {

                var currentScreen by remember { mutableStateOf("login") }
                var selectedChatUser by remember { mutableStateOf<User?>(null) }
                var selectedGroup by remember { mutableStateOf<Group?>(null) }
                when (currentScreen) {

                    "login" -> LoginScreen(
                        onLoginSuccess = {
                            currentScreen = "home"
                        },
                        onGoToRegister = {
                            currentScreen = "register"
                        }
                    )

                    "register" -> RegisterScreen(
                        onRegisterSuccess = {
                            currentScreen = "home"
                        }
                    )

                    "home" -> {
                        HomeScreen(
                            onUserClick = { selectedUser ->
                                selectedChatUser = selectedUser
                                currentScreen = "chat"
                            },
                            onGroupClick = { group ->
                                selectedGroup = group
                                currentScreen = "groupChat"

                            }
                        )
                    }

                    "chat" -> {
                        selectedChatUser?.let { user ->
                            ChatScreen(
                                user = user,
                                onBackClick = {
                                    currentScreen = "home"   // 👈 go back to HomeScreen
                                }
                            )
                        }
                    }
                    "groupChat" -> {
                        selectedGroup?.let { group ->
                            GroupChatScreen(
                                group = group,
                                onBackClick = {
                                    currentScreen = "home"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChattyTheme {
        Greeting("Android")
    }
}