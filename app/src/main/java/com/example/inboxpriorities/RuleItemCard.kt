package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RuleItemCard(
    rule: PriorityRule,
    onToggleEnable: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                Text(
                    text = "Pattern: \"${rule.matchPattern}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Priority Badge Chip
                PriorityBadge(priorityLevel = rule.priorityLevel)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side: Active Switch
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggleEnable
            )
        }
    }
}

@Composable
private fun PriorityBadge(priorityLevel: PriorityLevel) {
    val (backgroundColor, contentColor) = when (priorityLevel) {
        PriorityLevel.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        PriorityLevel.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PriorityLevel.LOW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        PriorityLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = priorityLevel.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        priorityLevel = PriorityLevel.HIGH,
        isEnabled = true
    )
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RuleItemCard(
                rule = sampleRule,
                onToggleEnable = {}
            )
        }
    }
}
