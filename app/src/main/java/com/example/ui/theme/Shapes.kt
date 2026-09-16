package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * IDDET Design System (shadcn/ui inspired) - 8px (8.dp) rounded corners everywhere:
 * boutons, cartes, champs, dialogues, barres, menus, etc.
 */
val ShadcnRadius = 8.dp
val ShadcnShape = RoundedCornerShape(8.dp)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)
