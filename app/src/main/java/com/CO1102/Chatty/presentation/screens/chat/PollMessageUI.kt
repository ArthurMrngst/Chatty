package com.CO1102.Chatty.presentation.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.CO1102.Chatty.domain.model.Message
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PollMessageUI(
    message: Message,
    currentUserId: String,
    onVote: (Map<String, String>, Map<String, Long>) -> Unit
) {

    Column(modifier = Modifier.padding(8.dp)) {

        Text(
            text = message.pollQuestion,
            style = MaterialTheme.typography.titleMedium
        )

        val totalVotes = message.pollOptions.values.sum()

        message.pollOptions.forEach { (option, countLong) ->

            val count = countLong
            val votedOption = message.pollVotes[currentUserId]

            val percentage =
                if (totalVotes == 0L) 0f
                else count.toFloat() / totalVotes.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {

                        val newVotes = message.pollVotes.toMutableMap()
                        val newOptions = message.pollOptions.toMutableMap()

                        // ✅ remove old vote safely
                        if (votedOption != null && newOptions.containsKey(votedOption)) {
                            val oldCount = newOptions[votedOption] ?: 0L
                            newOptions[votedOption] = maxOf(0L, oldCount - 1L)
                        }

                        // ✅ add new vote
                        newVotes[currentUserId] = option
                        newOptions[option] =
                            (newOptions[option] ?: 0L) + 1L

                        // 🔥 send to ViewModel instead
                        onVote(newVotes, newOptions)
                    }
                    .padding(vertical = 6.dp)
            ) {

                Text("$option ($count)")

                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text("Total votes: $totalVotes")
    }
}