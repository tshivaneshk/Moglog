package com.skech.moglog.ui.designsystem

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/**
 * iOS-tuned spring specifications and reduce-motion helpers.
 */
object MoglogMotion {
    fun <T> navigationSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> sheetSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> pressSpring() = spring<T>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium
    )

    fun isReduceMotionEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Tactile iOS-like haptic feedback dispatcher with API level checks.
 */
class MoglogHaptics(
    private val view: View,
    private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun lightImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun selectionChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            lightImpact()
        }
    }

    fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 25), -1))
        } else {
            longPress()
        }
    }
}

@Composable
fun rememberMoglogHaptics(): MoglogHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) { MoglogHaptics(view, context) }
}

/**
 * iOS press feedback modifier: scales to [pressedScale] (0.97f default) and slightly dims alpha (0.88f)
 * with no bounce. Respects system Reduce Motion setting.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.iosClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    hapticOnClick: Boolean = false,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val haptics = rememberMoglogHaptics()
    val reduceMotion = remember(context) { MoglogMotion.isReduceMotionEnabled(context) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) pressedScale else 1f,
        animationSpec = if (reduceMotion) tween(0) else MoglogMotion.pressSpring(),
        label = "ios_press_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.86f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "ios_press_alpha"
    )

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = {
                if (hapticOnClick) haptics.lightImpact()
                onClick()
            },
            onLongClick = {
                haptics.longPress()
                onLongClick()
            }
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = {
                if (hapticOnClick) haptics.lightImpact()
                onClick()
            }
        )
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .then(clickModifier)
}
