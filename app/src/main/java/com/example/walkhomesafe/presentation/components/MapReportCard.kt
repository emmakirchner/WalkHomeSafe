package com.example.walkhomesafe.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.walkhomesafe.api.ReportDto

/**
 * Card for displaying a safety report in the report list on the map screen.
 *
 * Contains title, author, date, vote buttons (upvote/downvote),
 * "On map" button and "Details" button.
 *
 * @param report The report to display
 * @param userVote Current user's vote (true = upvote, false = downvote, null = none)
 * @param onVote Callback on vote click (true = upvote, false = downvote)
 * @param isSelected true if the report is marked on the map
 * @param onToggleSelect Callback to toggle the map marker
 */
@Composable
fun MapReportCard(report: ReportDto, userVote: Boolean?, onVote: (Boolean) -> Unit, isSelected: Boolean, onToggleSelect: () -> Unit) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${report.userName} · ${report.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onVote(true) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (userVote == true) Icons.Filled.ThumbUp
                        else Icons.Outlined.ThumbUp,
                        contentDescription = "Upvote",
                        tint = if (userVote == true) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = report.upvoteCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { onVote(false) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (userVote == false) Icons.Filled.ThumbDown
                        else Icons.Outlined.ThumbDown,
                        contentDescription = "Downvote",
                        tint = if (userVote == false) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = report.downvoteCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onToggleSelect) {
                    Text(
                        text = "Auf Karte",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showDetails = true }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
            }
        }
    }

    if (showDetails) {
        ReportDetailsDialog(
            report = report,
            userVote = userVote,
            onVote = onVote,
            onDismiss = { showDetails = false }
        )
    }
}

/**
 * Detail dialog for a report with complete information.
 *
 * Shows title, author, date, description, vote buttons, and
 * category ratings as star display.
 *
 * @param report The report to display
 * @param userVote Current user's vote (true = upvote, false = downvote, null = none)
 * @param onVote Callback on vote click
 * @param onDismiss Callback to close the dialog
 */
@Composable
private fun ReportDetailsDialog(
    report: ReportDto,
    userVote: Boolean?,
    onVote: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${report.userName} · ${report.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (report.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onVote(true) }) {
                        Icon(
                            imageVector = if (userVote == true) Icons.Filled.ThumbUp
                            else Icons.Outlined.ThumbUp,
                            contentDescription = "Upvote",
                            tint = if (userVote == true) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = report.upvoteCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { onVote(false) }) {
                        Icon(
                            imageVector = if (userVote == false) Icons.Filled.ThumbDown
                            else Icons.Outlined.ThumbDown,
                            contentDescription = "Downvote",
                            tint = if (userVote == false) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = report.downvoteCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!report.ratingCategories.isNullOrEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    report.ratingCategories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${category.name}: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StarRating(rating = category.rating)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
