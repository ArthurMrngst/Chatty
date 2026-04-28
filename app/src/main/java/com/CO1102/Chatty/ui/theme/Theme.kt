package com.CO1102.Chatty.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Messenger Colors ───────────────────────────────────────────────────────
val MessengerBlue       = Color(0xFF0084FF)
val MessengerBlueLight  = Color(0xFF0099FF)
val MessengerBubbleMe   = Color(0xFF0084FF)
val MessengerBubbleOther= Color(0xFFE4E6EB)
val MessengerBackground = Color(0xFFF0F2F5)
val MessengerSurface    = Color(0xFFFFFFFF)
val MessengerTextPrimary   = Color(0xFF050505)
val MessengerTextSecondary = Color(0xFF65676B)
val MessengerOnline     = Color(0xFF31A24C)
val MessengerError      = Color(0xFFFA383E)
val MessengerDivider    = Color(0xFFE4E6EB)

// ── Color Scheme ───────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = MessengerBlue,
    onPrimary        = Color.White,
    primaryContainer = MessengerBlueLight,
    secondary        = MessengerBlue,
    background       = MessengerSurface,
    surface          = MessengerSurface,
    surfaceVariant   = MessengerBubbleOther,
    onSurface        = MessengerTextPrimary,
    onSurfaceVariant = MessengerTextPrimary,
    outline          = MessengerDivider,
    error            = MessengerError,
)

// ── Theme wrapper ──────────────────────────────────────────────────────────
@Composable
fun ChattyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content     = content
    )
}
