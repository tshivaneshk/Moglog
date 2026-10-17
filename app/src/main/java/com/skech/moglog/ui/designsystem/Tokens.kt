package com.skech.moglog.ui.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Semantic iOS-inspired color tokens for Moglog (Dark default + Light theme).
 * Avoids hard-coded Color(0xFF...) values across screens.
 */
@Immutable
data class MoglogColors(
    val isDark: Boolean,
    val background: Color,
    val groupedBackground: Color,
    val secondaryBackground: Color,
    val tertiaryBackground: Color,
    val elevatedSurface: Color,
    val frostedBarSurface: Color,
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val accent: Color,
    val accentSubtle: Color,
    val onAccent: Color,
    val statusWatching: Color,
    val statusPlanned: Color,
    val statusDone: Color,
    val destructive: Color,
    val warning: Color,
    val starGold: Color,
    val hairlineWidth: Dp = 0.5.dp
) {
    fun statusColor(status: String): Color = when (status) {
        "Watching" -> statusWatching
        "Planned" -> statusPlanned
        "Done" -> statusDone
        else -> accent
    }
}

val DarkMoglogColors = MoglogColors(
    isDark = true,
    background = Color(0xFF000000),
    groupedBackground = Color(0xFF000000),
    secondaryBackground = Color(0xFF1C1C1E),
    tertiaryBackground = Color(0xFF2C2C2E),
    elevatedSurface = Color(0xFF242426),
    frostedBarSurface = Color(0xCC161618),
    separator = Color(0xFF38383A),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5), // 60% alpha
    tertiaryLabel = Color(0x4DEBEBF5),  // 30% alpha
    accent = Color(0xFF30D158),         // iOS System Green (Dark)
    accentSubtle = Color(0x2630D158),
    onAccent = Color(0xFF000000),
    statusWatching = Color(0xFFFF9F0A), // iOS System Orange
    statusPlanned = Color(0xFF0A84FF),  // iOS System Blue
    statusDone = Color(0xFF30D158),     // iOS System Green
    destructive = Color(0xFFFF453A),    // iOS System Red
    warning = Color(0xFFFFD60A),
    starGold = Color(0xFFFFD60A)
)

val LightMoglogColors = MoglogColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    groupedBackground = Color(0xFFF2F2F7),
    secondaryBackground = Color(0xFFFFFFFF),
    tertiaryBackground = Color(0xFFE5E5EA),
    elevatedSurface = Color(0xFFFFFFFF),
    frostedBarSurface = Color(0xCCF9F9FB),
    separator = Color(0xFFC6C6C8),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43), // 60% alpha
    tertiaryLabel = Color(0x4D3C3C43),  // 30% alpha
    accent = Color(0xFF34C759),         // iOS System Green (Light)
    accentSubtle = Color(0x2234C759),
    onAccent = Color(0xFFFFFFFF),
    statusWatching = Color(0xFFFF9500),
    statusPlanned = Color(0xFF007AFF),
    statusDone = Color(0xFF34C759),
    destructive = Color(0xFFFF3B30),
    warning = Color(0xFFFFCC00),
    starGold = Color(0xFFFFB800)
)

val LocalMoglogColors = staticCompositionLocalOf { DarkMoglogColors }
