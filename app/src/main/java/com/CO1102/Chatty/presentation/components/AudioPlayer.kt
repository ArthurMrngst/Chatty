package com.CO1102.Chatty.presentation.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun AudioPlayer(url: String) {
    // Track player state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // Clean up player when composable leaves screen
    DisposableEffect(url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        // Play / Pause / Stop button
        Button(
            onClick = {
                if (isPlaying) {
                    // STOP
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isPlaying = false
                } else {
                    // PLAY
                    isLoading = true
                    errorMsg = ""
                    try {
                        val player = MediaPlayer().apply {
                            setDataSource(url)
                            setOnPreparedListener {
                                isLoading = false
                                isPlaying = true
                                start()
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                release()
                                mediaPlayer = null
                            }
                            setOnErrorListener { _, _, _ ->
                                isLoading = false
                                isPlaying = false
                                errorMsg = "Playback failed"
                                true
                            }
                            prepareAsync() // non-blocking — safe for network URLs
                        }
                        mediaPlayer = player
                    } catch (e: Exception) {
                        isLoading = false
                        errorMsg = "Cannot play audio"
                    }
                }
            },
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = when {
                    isLoading -> "⏳"
                    isPlaying -> "⏹"
                    else -> "▶"
                }
            )
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = when {
                    isLoading -> "Loading..."
                    isPlaying -> "Playing..."
                    else -> "🎧 Voice message"
                },
                style = MaterialTheme.typography.bodySmall
            )
            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}