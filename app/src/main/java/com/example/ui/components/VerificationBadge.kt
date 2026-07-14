package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VerificationBadge(
    modifier: Modifier = Modifier,
    showExplainingOnClick: Boolean = true,
    userName: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    val badgeColor = remember(userName) {
        when (userName?.lowercase()) {
            "doffranel" -> Color(0xFF06B6D4) // Cyan
            "c.m.o" -> Color(0xFF8B5CF6) // Violet
            "crislem" -> Color(0xFFFACC15) // Yellow
            else -> Color(0xFFDC2626) // Default Red
        }
    }

    Box(
        modifier = modifier
            .size(16.dp)
            .background(badgeColor, CircleShape)
            .let {
                if (showExplainingOnClick) {
                    it.clickable { showDialog = true }
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Verified",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }

    if (showDialog) {
        VerificationExplanationDialog(
            userName = userName,
            badgeColor = badgeColor,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun VerificationExplanationDialog(
    userName: String?,
    badgeColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(badgeColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (userName?.lowercase() in listOf("doffranel", "c.m.o", "crislem")) "Badge Créateur" else "Compte Vérifié",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val description = when (userName?.lowercase()) {
                    "doffranel" -> "Badge Cyan: Compte officiel de Doffranel, créateur et support."
                    "c.m.o" -> "Badge Violet: Compte officiel de C.M.O, administrateur système."
                    "crislem" -> "Badge Jaune: Compte officiel de Crislem, support client."
                    else -> if (userName != null) {
                        "Le badge de vérification confirme que le compte de @$userName a été officiellement vérifié."
                    } else {
                        "Le badge de vérification confirme que ce compte a été officiellement vérifié."
                    }
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ce badge indique un statut spécial au sein de la communauté IDDET.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = badgeColor)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = true
        )
    )
}
