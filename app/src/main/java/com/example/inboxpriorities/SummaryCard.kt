package com.example.inboxpriorities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OptionASummaryCard(
    totalUnread: Int,
    highCount: Int,
    medCount: Int,
    lowCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Unread Email Summary",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total Count
                Column {
                    Text(
                        text = "$totalUnread",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Unread",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Granular Priority Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityCountPill(count = highCount, label = "High", color = Color(0xFFD32F2F))
                    PriorityCountPill(count = medCount, label = "Med", color = Color(0xFFE65100))
                    PriorityCountPill(count = lowCount, label = "Low", color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

@Composable
fun PriorityCountPill(count: Int, label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count ",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = color
            )
        }
    }
}