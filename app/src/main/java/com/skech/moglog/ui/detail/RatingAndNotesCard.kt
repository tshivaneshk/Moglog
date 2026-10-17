package com.skech.moglog.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skech.moglog.R
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Native Compose Personal Rating (0–10 in 0.5 increments) and Per-Show Notes + Tags card.
 * Replaces the old WebView HTML global note screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingAndNotesCard(
    myRating: Double,
    onRatingChange: (Double) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Personal Rating Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MoglogShapes.Card)
                .background(colors.secondaryBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = MoglogIcons.StarFilled,
                        contentDescription = null,
                        tint = colors.starGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.detail_my_rating_title),
                        style = typo.headline,
                        color = colors.label
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (myRating > 0.0) {
                            String.format(Locale.US, "%.1f / 10", myRating)
                        } else {
                            stringResource(R.string.detail_not_rated)
                        },
                        style = typo.callout.copy(fontWeight = FontWeight.Bold),
                        color = if (myRating > 0.0) accentColor else colors.secondaryLabel
                    )
                    if (myRating > 0.0) {
                        Text(
                            text = "Clear",
                            style = typo.caption,
                            color = colors.secondaryLabel,
                            modifier = Modifier.iosClickable(
                                onClick = {
                                    haptics.lightImpact()
                                    onRatingChange(0.0)
                                }
                            )
                        )
                    }
                }
            }

            // Half-step 0..10 slider
            Slider(
                value = myRating.toFloat(),
                onValueChange = { raw ->
                    val stepped = (raw * 2f).roundToInt() / 2.0
                    if (stepped != myRating) {
                        haptics.selectionChanged()
                        onRatingChange(stepped)
                    }
                },
                valueRange = 0f..10f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = colors.tertiaryBackground
                )
            )
        }

        // Native Notes & Tags Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MoglogShapes.Card)
                .background(colors.secondaryBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = MoglogIcons.Note,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.detail_notes_title),
                    style = typo.headline,
                    color = colors.label
                )
            }

            // Multi-line Notes Editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(SmoothRoundedCornerShape(10.dp))
                    .background(colors.tertiaryBackground)
                    .padding(12.dp)
            ) {
                if (notes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_notes_placeholder),
                        style = typo.subhead,
                        color = colors.tertiaryLabel
                    )
                }
                BasicTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    textStyle = typo.subhead.copy(color = colors.label),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(thickness = colors.hairlineWidth, color = colors.separator)

            // Tags Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmoothRoundedCornerShape(10.dp))
                    .background(colors.tertiaryBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_tags_placeholder),
                        style = typo.footnote,
                        color = colors.tertiaryLabel
                    )
                }
                BasicTextField(
                    value = tags,
                    onValueChange = onTagsChange,
                    singleLine = true,
                    textStyle = typo.footnote.copy(color = colors.label),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val parsedTags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (parsedTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    parsedTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(SmoothRoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = typo.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
