package com.CO1102.Chatty.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.CO1102.Chatty.domain.model.Message
@Composable
fun AdminElectionUI(
    message: Message,
    currentUserId: String,
    totalMembers: Int,
    onVote: (approve: Boolean) -> Unit
) {
    val hasVoted = message.electionYesVotes.contains(currentUserId)
            || message.electionNoVotes.contains(currentUserId)

    val yesCount = message.electionYesVotes.size
    val noCount  = message.electionNoVotes.size
    val majority = (totalMembers / 2) + 1

    // Determine outcome
    val isElected  = message.electionResolved && yesCount >= majority
    val isRejected = message.electionResolved && yesCount < majority

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🗳️", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Admin Election",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Nominate: ${message.electionNomineeDisplay}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Nominated by: ${message.electionNominatorDisplay}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            // Vote tally bar
            val total = (yesCount + noCount).coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { yesCount.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✅ $yesCount yes", style = MaterialTheme.typography.labelSmall)
                Text("Needs $majority/$totalMembers", style = MaterialTheme.typography.labelSmall)
                Text("❌ $noCount no", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(10.dp))

            // Result or voting buttons
            when {
                isElected -> {
                    Text(
                        text = "🎉 ${message.electionNomineeDisplay} is now an admin!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                isRejected -> {
                    Text(
                        text = "❌ Nomination rejected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                hasVoted -> {
                    Text(
                        text = "✔ You have voted. Waiting for others...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onVote(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✅ Approve")
                        }
                        OutlinedButton(
                            onClick = { onVote(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("❌ Reject")
                        }
                    }
                }
            }
        }
    }
}