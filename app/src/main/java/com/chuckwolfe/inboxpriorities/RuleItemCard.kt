package com.chuckwolfe.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RuleItemCard(
    rule: PriorityRule,
    onToggleEnable: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (rule.isEnabled) 1.0f else 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Rule Info & Priority Chip
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val ruleTypeLabel = when (rule.ruleType) {
                    RuleType.SENDER_CONTAINS -> "Sender contains"
                    RuleType.SUBJECT_CONTAINS -> "Subject contains"
                }

                Text(
                    text = "$ruleTypeLabel \"${rule.matchPattern}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                PriorityBadge(priorityLevel = rule.priorityLevel)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Side: Action Buttons & Toggle Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEditClick) {
                    Text("✏️", style = MaterialTheme.typography.titleMedium)
                }

                IconButton(onClick = onDeleteClick) {
                    Text("🗑️", style = MaterialTheme.typography.titleMedium)
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnable
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priorityLevel: PriorityLevel) {
    val (backgroundColor, contentColor) = when (priorityLevel) {
        PriorityLevel.HIGH -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        PriorityLevel.MEDIUM -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        PriorityLevel.LOW -> Color(0xFFE3F2FD) to Color(0xFF1976D2) // Updated to match dialog blue
        PriorityLevel.NONE -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priorityLevel.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RuleItemCardPreview() {
    val sampleRule = PriorityRule(
        name = "Client Emails",
        ruleType = RuleType.SENDER_CONTAINS,
        matchPattern = "@client.com",
        priorityLevel = PriorityLevel.LOW,
        isEnabled = true
    )
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RuleItemCard(
                rule = sampleRule,
                onToggleEnable = {},
                onEditClick = {},
                onDeleteClick = {}
            )
        }
    }
}