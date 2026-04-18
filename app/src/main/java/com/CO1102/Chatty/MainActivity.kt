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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(
                    mapOf("online" to true),
                    SetOptions.merge() // ✅ FIX
                )
        }

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
                                    currentScreen = "home"
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

    override fun onStart() {
        super.onStart()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("online" to true),
                SetOptions.merge() // ✅ FIX
            )
    }

    override fun onStop() {
        super.onStop()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "online" to false,
                    "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                SetOptions.merge() // ✅ FIX
            )
    }

    override fun onDestroy() {
        super.onDestroy()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("online" to false),
                SetOptions.merge() // ✅ FIX
            )
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