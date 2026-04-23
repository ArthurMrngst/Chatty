

package com.CO1102.Chatty.presentation.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AudioPlayer(
    audioUrl: String
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 🔥 Clean up when composable leaves screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Release error: ${e.message}")
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {

        Button(
            onClick = {

                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {

                        try {
                            setDataSource(audioUrl)

                            setOnPreparedListener {
                                isPrepared = true
                                start()
                                isPlaying = true
                            }

                            setOnCompletionListener {
                                isPlaying = false
                            }

                            prepareAsync()

                        } catch (e: Exception) {
                            Log.e("AudioPlayer", "Error: ${e.message}")
                        }
                    }
                } else {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.start()
                            isPlaying = true
                        }
                    }
                }
            }
        ) {
            Text(
                when {
                    !isPrepared && mediaPlayer != null -> "Loading..."
                    isPlaying -> "⏸ Pause"
                    else -> "▶ Play"
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                mediaPlayer?.apply {
                    stop()
                    release()
                }
                mediaPlayer = null
                isPlaying = false
                isPrepared = false
            }
        ) {
            Text("⏹ Stop")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text("Voice message")
    }
}