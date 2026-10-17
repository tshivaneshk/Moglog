package com.skech.moglog.ui.onboarding

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skech.moglog.R
import com.skech.moglog.ui.designsystem.IosBottomSheet
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosPrimaryButton
import com.skech.moglog.ui.designsystem.IosSecondaryButton
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics

/**
 * 3-step paged iOS-style Onboarding screen:
 * Step 0: Welcome & clean Moglog wordmark
 * Step 1: Add TMDB API Key / Read Access Token with Paste button & inline instructions sheet
 * Step 2: Optional Google Sign-In for E2E encrypted backup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialKey: String = "",
    isValidKey: (String) -> Boolean,
    onSaveKey: (String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    isSignedIn: Boolean = false,
    signedInEmail: String? = null,
    onFinish: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(0) }
    var apiKeyInput by remember { mutableStateOf(initialKey) }
    var keyError by remember { mutableStateOf<String?>(null) }
    var showHowToSheet by remember { mutableStateOf(false) }

    if (showHowToSheet) {
        IosBottomSheet(onDismissRequest = { showHowToSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_how_to_get_key),
                    style = typo.title2,
                    color = colors.label
                )
                IosGroupedSection {
                    IosGroupedRow(
                        title = "1. Create a free TMDB account",
                        subtitle = "Tap to open themoviedb.org/signup in your browser",
                        icon = MoglogIcons.ExternalLink,
                        showChevron = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/signup"))
                            context.startActivity(intent)
                        }
                    )
                    IosGroupedRow(
                        title = "2. Open Settings → API",
                        subtitle = "Request a free Developer API key (choose 'Mobile Application').",
                        icon = MoglogIcons.Key
                    )
                    IosGroupedRow(
                        title = "3. Copy your API Key or Read Token",
                        subtitle = "Both 32-character v3 keys and v4 Bearer tokens are supported and stored encrypted with Android Keystore.",
                        icon = MoglogIcons.Shield,
                        showDivider = false
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                IosPrimaryButton(
                    text = "Got It",
                    onClick = { showHowToSheet = false }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Step indicators + Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { step ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (step == currentStep) 22.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (step <= currentStep) colors.accent else colors.tertiaryBackground)
                    )
                }
            }

            Text(
                text = stringResource(R.string.onboarding_skip),
                style = typo.callout.copy(fontWeight = FontWeight.Medium),
                color = colors.secondaryLabel,
                modifier = Modifier
                    .iosClickable(hapticOnClick = true, onClick = onFinish)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Step Content
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "onboarding_step"
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (step) {
                    0 -> {
                        // Clean iOS wordmark badge
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(SmoothRoundedCornerShape(24.dp))
                                .background(colors.accentSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MoglogIcons.Library,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Moglog",
                            style = typo.largeTitle.copy(
                                fontSize = 40.sp,
                                letterSpacing = (-1.0).sp
                            ),
                            color = colors.label
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.onboarding_step1_title),
                            style = typo.title3,
                            color = colors.accent,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.onboarding_step1_subtitle),
                            style = typo.body,
                            color = colors.secondaryLabel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        IosGroupedSection {
                            IosGroupedRow(
                                title = "Season & Episode Stepper",
                                subtitle = "One-tap +1 episode from your Library or Home Widget",
                                icon = MoglogIcons.Play
                            )
                            IosGroupedRow(
                                title = "Personal Ratings & Notes",
                                subtitle = "Write thoughts, tags, and track completion streaks",
                                icon = MoglogIcons.Note
                            )
                            IosGroupedRow(
                                title = "Encrypted at Rest",
                                subtitle = "SQLCipher database & Android Keystore protection",
                                icon = MoglogIcons.Lock,
                                showDivider = false
                            )
                        }
                    }

                    1 -> {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(SmoothRoundedCornerShape(22.dp))
                                .background(colors.accentSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MoglogIcons.Key,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.onboarding_step2_title),
                            style = typo.title1,
                            color = colors.label,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.onboarding_step2_subtitle),
                            style = typo.subhead,
                            color = colors.secondaryLabel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Key Input Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MoglogShapes.Card)
                                .background(colors.secondaryBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (keyError != null) colors.destructive else colors.separator,
                                    shape = MoglogShapes.Card
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            if (apiKeyInput.isEmpty()) {
                                Text(
                                    text = "Paste 32-char v3 key or v4 Bearer token…",
                                    style = typo.callout,
                                    color = colors.tertiaryLabel
                                )
                            }
                            BasicTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    keyError = null
                                },
                                textStyle = typo.callout.copy(color = colors.label),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (keyError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = keyError!!,
                                style = typo.footnote,
                                color = colors.destructive
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IosSecondaryButton(
                                text = stringResource(R.string.onboarding_paste_clipboard),
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                                    if (text.isNotEmpty()) {
                                        apiKeyInput = text
                                        keyError = null
                                        haptics.success()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.onboarding_how_to_get_key),
                            style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent,
                            modifier = Modifier
                                .iosClickable(hapticOnClick = true, onClick = { showHowToSheet = true })
                                .padding(8.dp)
                        )
                    }

                    2 -> {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(SmoothRoundedCornerShape(22.dp))
                                .background(colors.accentSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MoglogIcons.Cloud,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.onboarding_step3_title),
                            style = typo.title1,
                            color = colors.label,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.onboarding_step3_subtitle),
                            style = typo.subhead,
                            color = colors.secondaryLabel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isSignedIn) {
                            IosGroupedSection {
                                IosGroupedRow(
                                    title = "Signed In",
                                    subtitle = signedInEmail ?: "Google Account connected",
                                    icon = MoglogIcons.Check,
                                    showDivider = false
                                )
                            }
                        } else {
                            IosSecondaryButton(
                                text = stringResource(R.string.settings_sign_in_google),
                                leadingIcon = MoglogIcons.Person,
                                onClick = onGoogleSignInClick
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Primary CTA
        IosPrimaryButton(
            text = when (currentStep) {
                0 -> stringResource(R.string.onboarding_continue)
                1 -> if (apiKeyInput.isBlank()) "Continue Without Key" else "Save Key & Continue"
                else -> stringResource(R.string.onboarding_get_started)
            },
            onClick = {
                when (currentStep) {
                    0 -> {
                        haptics.lightImpact()
                        currentStep = 1
                    }
                    1 -> {
                        val clean = apiKeyInput.trim()
                        if (clean.isNotEmpty() && !isValidKey(clean)) {
                            keyError = "Enter a valid 32-character hex API key or v4 JWT Read Access Token."
                            haptics.warning()
                        } else {
                            if (clean.isNotEmpty()) {
                                onSaveKey(clean)
                                haptics.success()
                            }
                            currentStep = 2
                        }
                    }
                    else -> {
                        haptics.success()
                        onFinish()
                    }
                }
            }
        )
    }
}
