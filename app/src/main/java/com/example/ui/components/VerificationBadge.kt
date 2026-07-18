package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Verification state representing different levels of verification
enum class VerificationState {
    OFFICIAL,
    VERIFIED,
    NONE
}

// Special usernames that get the Niveau 1 White badge (Official)
private val FOUNDER_USERNAMES = listOf("C.M.O", "Doffranel", "Crislem", "Mauricio-100")

/**
 * Resolves the verification state based on username and verification flag.
 */
fun getVerificationState(userName: String?, isVerified: Boolean): VerificationState {
    val name = userName ?: ""
    return when {
        name in FOUNDER_USERNAMES -> VerificationState.OFFICIAL
        isVerified -> VerificationState.VERIFIED
        else -> VerificationState.NONE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationBadge(
    modifier: Modifier = Modifier,
    userName: String? = null,
    isVerified: Boolean = false,
    state: VerificationState? = null, // Can be explicitly passed
    showExplainingOnClick: Boolean = true
) {
    val resolvedState = state ?: getVerificationState(userName, isVerified)
    if (resolvedState == VerificationState.NONE) return

    var showBottomSheet by remember { mutableStateOf(false) }
    val name = userName ?: ""

    // Wrap in a box with a minimum interactive component size of 48.dp for accessibility (minimum touch targets)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(enabled = showExplainingOnClick) { showBottomSheet = true }
            .testTag("verification_badge_click_area_$name"),
        contentAlignment = Alignment.Center
    ) {
        when (resolvedState) {
            VerificationState.OFFICIAL -> {
                // Niveau 1 (Badge Blanc Officiel) : White background, subtle dark contour, dark checkmark
                Box(
                    modifier = modifier
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                        .testTag("verification_badge_founder_$name"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Badge officiel $name",
                        tint = Color(0xFF111827), // Dark Gray/Black for perfect visibility
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            VerificationState.VERIFIED -> {
                // Niveau 2 (Badge Bleu Standard) : Blue background (#1DA1F2), white checkmark
                Box(
                    modifier = modifier
                        .background(Color(0xFF1DA1F2), CircleShape)
                        .testTag("verification_badge_verified_$name"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Compte vérifié",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            VerificationState.NONE -> {
                // Should not reach here
            }
        }
    }

    if (showBottomSheet) {
        VerificationBottomSheet(
            userName = name,
            verificationState = resolvedState,
            onDismiss = { showBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationBottomSheet(
    userName: String,
    verificationState: VerificationState,
    onDismiss: () -> Unit
) {
    if (verificationState == VerificationState.NONE) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("verification_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon / Badge representation
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = if (verificationState == VerificationState.OFFICIAL) Color.White else Color(0xFF1DA1F2),
                        shape = CircleShape
                    )
                    .border(
                        width = if (verificationState == VerificationState.OFFICIAL) 2.dp else 0.dp,
                        color = Color.Black.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (verificationState == VerificationState.OFFICIAL) Color(0xFF111827) else Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = if (verificationState == VerificationState.OFFICIAL) "Compte officiel $userName" else "Compte vérifié",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Description text
            Text(
                text = if (verificationState == VerificationState.OFFICIAL) {
                    "Ce badge distingue les comptes fondateurs et l'équipe officielle de C.M.O."
                } else {
                    "Ce compte est vérifié car il remplit les critères d'Iddet : profil complet, +10k abonnés, +100k vues cumulées, 18+ ans."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verification_sheet_close_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Compris",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
