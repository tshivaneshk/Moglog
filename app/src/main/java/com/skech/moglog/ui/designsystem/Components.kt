package com.skech.moglog.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Translucent iOS frosted-glass top/bottom bar container with hairline border.
 */
@Composable
fun IosFrostedBar(
    modifier: Modifier = Modifier,
    showTopBorder: Boolean = false,
    showBottomBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMoglogColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.frostedBarSurface)
    ) {
        if (showTopBorder) {
            HorizontalDivider(thickness = colors.hairlineWidth, color = colors.separator)
        }
        content()
        if (showBottomBorder) {
            HorizontalDivider(thickness = colors.hairlineWidth, color = colors.separator)
        }
    }
}

/**
 * iOS-style navigation bar that transitions smoothly between a Large Title header and an inline centered title.
 */
@Composable
fun IosLargeTitleHeader(
    title: String,
    collapsed: Boolean = false,
    leadingActions: @Composable (RowScope.() -> Unit)? = null,
    trailingActions: @Composable (RowScope.() -> Unit)? = null,
    bottomContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    IosFrostedBar(showBottomBorder = collapsed) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    leadingActions?.invoke(this)
                }

                AnimatedVisibility(
                    visible = collapsed,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(120))
                ) {
                    Text(
                        text = title,
                        style = typo.headline,
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    trailingActions?.invoke(this)
                }
            }

            AnimatedVisibility(visible = !collapsed) {
                Text(
                    text = title,
                    style = typo.largeTitle,
                    color = colors.label,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            }

            if (bottomContent != null) {
                bottomContent()
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Circular translucent icon button used in iOS navigation bars and full-bleed detail headers.
 */
@Composable
fun IosCircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalMoglogColors.current.label,
    containerColor: Color = LocalMoglogColors.current.tertiaryBackground.copy(alpha = 0.85f),
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .iosClickable(hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Full-width equal-segment iOS Segmented Control with an animated sliding pill thumb.
 */
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(MoglogShapes.Control)
            .background(colors.tertiaryBackground)
            .padding(2.dp)
    ) {
        val segmentWidth = if (items.isNotEmpty()) maxWidth / items.size else maxWidth
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = MoglogMotion.navigationSpring(),
            label = "segmented_thumb_offset"
        )

        // Sliding thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 2.dp,
                    shape = SmoothRoundedCornerShape(8.dp),
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(SmoothRoundedCornerShape(8.dp))
                .background(if (colors.isDark) Color(0xFF636366) else Color.White)
        )

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .iosClickable(
                            pressedScale = 0.98f,
                            onClick = {
                                if (!isSelected) {
                                    haptics.selectionChanged()
                                    onItemSelected(item)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        style = typo.footnote.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) colors.label else colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * iOS-style search field with rounded container, leading magnifier, clear button, and optional Cancel button.
 */
@Composable
fun IosSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    showCancelButton: Boolean = false,
    onCancel: (() -> Unit)? = null
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(MoglogShapes.Control)
                .background(colors.tertiaryBackground)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = MoglogIcons.Search,
                contentDescription = null,
                tint = colors.secondaryLabel,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = typo.callout,
                        color = colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = typo.callout.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryLabel.copy(alpha = 0.35f))
                        .iosClickable(hapticOnClick = true, onClick = { onQueryChange("") }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MoglogIcons.Close,
                        contentDescription = "Clear",
                        tint = colors.background,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showCancelButton && onCancel != null) {
            Text(
                text = "Cancel",
                style = typo.callout.copy(fontWeight = FontWeight.Medium),
                color = colors.accent,
                modifier = Modifier
                    .iosClickable(hapticOnClick = true, onClick = { onCancel?.invoke() })
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * iOS Inset Grouped List Section (header in uppercase footnote + rounded card container + optional footer).
 */
@Composable
fun IosGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header.uppercase(),
                style = typo.footnote.copy(fontWeight = FontWeight.Medium),
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MoglogShapes.Card)
                .background(colors.secondaryBackground),
            content = content
        )
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = typo.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
            )
        }
    }
}

/**
 * Individual row inside an [IosGroupedSection] with optional leading colored icon square,
 * title, subtitle, trailing value/chevron/switch, and inset hairline divider.
 */
@Composable
fun IosGroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Color.White,
    iconBackground: Color = LocalMoglogColors.current.accent,
    titleColor: Color = LocalMoglogColors.current.label,
    valueText: String? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .iosClickable(pressedScale = 0.99f, hapticOnClick = true, onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Column(modifier = rowModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(SmoothRoundedCornerShape(7.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typo.body,
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = typo.footnote,
                        color = colors.secondaryLabel
                    )
                }
            }

            if (valueText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = valueText,
                    style = typo.callout,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }

            if (showChevron) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = MoglogIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.tertiaryLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = colors.hairlineWidth,
                color = colors.separator,
                modifier = Modifier.padding(start = if (icon != null) 58.dp else 16.dp)
            )
        }
    }
}

@Composable
fun IosSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconBackground: Color = LocalMoglogColors.current.accent,
    showDivider: Boolean = true
) {
    val colors = LocalMoglogColors.current
    val haptics = rememberMoglogHaptics()

    IosGroupedRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconBackground = iconBackground,
        showDivider = showDivider,
        onClick = {
            haptics.lightImpact()
            onCheckedChange(!checked)
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptics.lightImpact()
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colors.tertiaryBackground,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}

/**
 * Primary full-width iOS button with 14dp continuous corners and scale-down press feedback.
 */
@Composable
fun IosPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LocalMoglogColors.current.accent,
    contentColor: Color = LocalMoglogColors.current.onAccent,
    leadingIcon: ImageVector? = null
) {
    val typo = LocalMoglogTypography.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(MoglogShapes.Card)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
            .iosClickable(enabled = enabled, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = typo.headline,
                color = contentColor
            )
        }
    }
}

@Composable
fun IosSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = LocalMoglogColors.current.accent,
    leadingIcon: ImageVector? = null
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(MoglogShapes.Card)
            .background(colors.tertiaryBackground)
            .iosClickable(enabled = enabled, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = typo.headline,
                color = contentColor
            )
        }
    }
}

/**
 * iOS Bottom Sheet wrapper with rounded top corners and subtle drag handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosBottomSheet(
    onDismissRequest: () -> Unit,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMoglogColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = MoglogShapes.TopSheet,
        containerColor = colors.secondaryBackground,
        contentColor = colors.label,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(colors.tertiaryLabel)
            )
        },
        content = content
    )
}

data class ActionSheetItem(
    val label: String,
    val isDestructive: Boolean = false,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

/**
 * iOS Action Sheet for destructive confirmations and multi-action menus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosActionSheet(
    title: String?,
    message: String? = null,
    actions: List<ActionSheetItem>,
    onDismissRequest: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    IosBottomSheet(onDismissRequest = onDismissRequest, skipPartiallyExpanded = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MoglogShapes.Card)
                    .background(colors.tertiaryBackground)
            ) {
                if (!title.isNullOrBlank() || !message.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!title.isNullOrBlank()) {
                            Text(
                                text = title,
                                style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.secondaryLabel,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!message.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                style = typo.footnote,
                                color = colors.secondaryLabel,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    HorizontalDivider(thickness = colors.hairlineWidth, color = colors.separator)
                }

                actions.forEachIndexed { index, action ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .iosClickable(
                                pressedScale = 0.99f,
                                onClick = {
                                    if (action.isDestructive) haptics.warning() else haptics.lightImpact()
                                    onDismissRequest()
                                    action.onClick()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (action.icon != null) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = if (action.isDestructive) colors.destructive else colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = action.label,
                                style = typo.title3.copy(
                                    fontWeight = if (action.isDestructive) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (action.isDestructive) colors.destructive else colors.accent
                            )
                        }
                    }
                    if (index < actions.lastIndex) {
                        HorizontalDivider(thickness = colors.hairlineWidth, color = colors.separator)
                    }
                }
            }

            // Separate Cancel button pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(MoglogShapes.Card)
                    .background(colors.tertiaryBackground)
                    .iosClickable(hapticOnClick = true, onClick = onDismissRequest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = typo.headline,
                    color = colors.accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

data class IosToastData(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * Floating iOS-style pill toast banner with optional Undo button.
 */
@Composable
fun IosToastBanner(
    toast: IosToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (toast != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .shadow(10.dp, MoglogShapes.Pill)
                    .clip(MoglogShapes.Pill)
                    .background(colors.elevatedSurface)
                    .border(colors.hairlineWidth, colors.separator, MoglogShapes.Pill)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = toast.message,
                    style = typo.subhead.copy(fontWeight = FontWeight.Medium),
                    color = colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (toast.actionLabel != null && toast.onAction != null) {
                    Text(
                        text = toast.actionLabel,
                        style = typo.subhead.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent,
                        modifier = Modifier.iosClickable(
                            onClick = {
                                haptics.success()
                                toast.onAction.invoke()
                                onDismiss()
                            }
                        )
                    )
                }
            }
        }
    }
}

/**
 * Friendly centered empty state with vector icon badge, title, subtitle, and optional CTA button.
 */
@Composable
fun IosEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(SmoothRoundedCornerShape(20.dp))
                .background(colors.accentSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = typo.title3,
            color = colors.label,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = typo.subhead,
            color = colors.secondaryLabel,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .clip(MoglogShapes.Pill)
                    .background(colors.accent)
                    .iosClickable(hapticOnClick = true, onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onAccent
                )
            }
        }
    }
}

/**
 * Skeleton shimmer placeholder box for loading states.
 */
@Composable
fun IosShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    val colors = LocalMoglogColors.current
    val transition = rememberInfiniteTransition(label = "ios_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ios_shimmer_alpha"
    )
    Box(
        modifier = modifier
            .clip(SmoothRoundedCornerShape(cornerRadius))
            .background(colors.tertiaryBackground.copy(alpha = alpha))
    )
}
