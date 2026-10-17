package com.skech.moglog.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skech.moglog.R

/**
 * Bundled Inter OFL font family configured with an iOS-native typographic hierarchy:
 * LargeTitle 34/41 Bold, Title1 28, Title2 22, Title3 20 SemiBold, Headline 17 SemiBold,
 * Body 17, Callout 16, Subhead 15, Footnote 13, Caption 12.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold)
)

@Immutable
data class MoglogTypography(
    val largeTitle: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.4).sp
    ),
    val title1: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    val title2: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    val title3: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.15).sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    val callout: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.08).sp
    ),
    val subhead: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.05).sp
    ),
    val footnote: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

val DefaultMoglogTypography = MoglogTypography()

val LocalMoglogTypography = staticCompositionLocalOf { DefaultMoglogTypography }

val MaterialInterTypography = Typography(
    displayLarge = DefaultMoglogTypography.largeTitle,
    headlineLarge = DefaultMoglogTypography.title1,
    headlineMedium = DefaultMoglogTypography.title2,
    headlineSmall = DefaultMoglogTypography.title3,
    titleLarge = DefaultMoglogTypography.title2,
    titleMedium = DefaultMoglogTypography.headline,
    titleSmall = DefaultMoglogTypography.subhead.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultMoglogTypography.body,
    bodyMedium = DefaultMoglogTypography.subhead,
    bodySmall = DefaultMoglogTypography.footnote,
    labelLarge = DefaultMoglogTypography.callout.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = DefaultMoglogTypography.footnote.copy(fontWeight = FontWeight.Medium),
    labelSmall = DefaultMoglogTypography.caption
)
