package com.skech.moglog.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skech.moglog.ui.designsystem.IosPrimaryButton
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape

/**
 * Full-screen iOS-inspired privacy & biometric lock shield:
 * - Masks app content when moving to background / App Switcher if App Lock or Hide in Recents is enabled.
 * - Requires Biometric or Device Credential unlock on cold start and after configurable background timeout.
 */
@Composable
fun AppLockOverlay(
    appLockEnabled: Boolean,
    timeoutSeconds: Int,
    hideInRecents: Boolean
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLocked by rememberSaveable(appLockEnabled) { mutableStateOf(appLockEnabled) }
    var isBackgrounded by remember { mutableStateOf(false) }
    var backgroundedAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var authError by remember { mutableStateOf<String?>(null) }

    val triggerBiometricPrompt = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

            val canAuth = BiometricManager.from(activity).canAuthenticate(authenticators)
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isLocked = false
                            authError = null
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            authError = errString.toString()
                        }
                    }
                )
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Moglog")
                    .setSubtitle("Authenticate to access your watchlist and notes")
                    .setAllowedAuthenticators(authenticators)
                    .build()
                runCatching { prompt.authenticate(promptInfo) }
            } else {
                // Device has no biometric or lock screen credential enrolled
                isLocked = false
            }
        }
    }

    DisposableEffect(lifecycleOwner, appLockEnabled, timeoutSeconds, hideInRecents) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (appLockEnabled || hideInRecents) {
                        isBackgrounded = true
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (appLockEnabled) {
                        backgroundedAtMs = System.currentTimeMillis()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    isBackgrounded = false
                    if (appLockEnabled && backgroundedAtMs > 0L) {
                        val elapsedSec = (System.currentTimeMillis() - backgroundedAtMs) / 1000L
                        if (elapsedSec >= timeoutSeconds) {
                            isLocked = true
                            triggerBiometricPrompt()
                        }
                        backgroundedAtMs = 0L
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(appLockEnabled) {
        if (appLockEnabled && isLocked) {
            triggerBiometricPrompt()
        } else if (!appLockEnabled) {
            isLocked = false
        }
    }

    AnimatedVisibility(
        visible = isLocked || (isBackgrounded && (appLockEnabled || hideInRecents)),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .clickable(enabled = true, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(SmoothRoundedCornerShape(22.dp))
                        .background(colors.accentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MoglogIcons.Lock,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Moglog is Locked",
                    style = typo.title2,
                    color = colors.label,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = authError ?: "Authenticate with Face, Fingerprint, or Device PIN to view your library.",
                    style = typo.subhead,
                    color = colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )

                if (isLocked) {
                    Spacer(modifier = Modifier.height(28.dp))
                    IosPrimaryButton(
                        text = "Unlock Moglog",
                        leadingIcon = MoglogIcons.Shield,
                        onClick = triggerBiometricPrompt
                    )
                }
            }
        }
    }
}
