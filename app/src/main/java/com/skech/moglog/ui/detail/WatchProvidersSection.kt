package com.skech.moglog.ui.detail

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skech.moglog.R
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.domain.model.StreamingProvider
import com.skech.moglog.ui.designsystem.IosBottomSheet
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosPrimaryButton
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable

data class CuratedStreamingApp(
    val name: String,
    val packageName: String,
    val fallbackUrl: String
)

val CURATED_STREAMING_APPS = listOf(
    CuratedStreamingApp("Netflix", "com.netflix.mediaclient", "https://www.netflix.com"),
    CuratedStreamingApp("Prime Video", "com.amazon.avod.thirdpartyclient", "https://www.primevideo.com"),
    CuratedStreamingApp("Disney+", "com.disney.disneyplus", "https://www.disneyplus.com"),
    CuratedStreamingApp("Disney+ Hotstar", "in.startv.hotstar", "https://www.hotstar.com"),
    CuratedStreamingApp("Max", "com.wbd.stream", "https://www.max.com"),
    CuratedStreamingApp("Hulu", "com.hulu.plus", "https://www.hulu.com"),
    CuratedStreamingApp("Apple TV", "com.apple.atve.androidtv.appletv", "https://tv.apple.com"),
    CuratedStreamingApp("Crunchyroll", "com.crunchyroll.crunchyroid", "https://www.crunchyroll.com"),
    CuratedStreamingApp("Rakuten Viki", "com.viki.android", "https://www.viki.com"),
    CuratedStreamingApp("iQIYI", "com.iqiyi.i18n", "https://www.iq.com"),
    CuratedStreamingApp("WeTV", "com.tencent.qqlivei18n", "https://wetv.vip"),
    CuratedStreamingApp("YouTube", "com.google.android.youtube", "https://www.youtube.com")
)

fun Drawable.toComposeImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.asImageBitmap()
    }
    val w = if (intrinsicWidth > 0) intrinsicWidth else 1
    val h = if (intrinsicHeight > 0) intrinsicHeight else 1
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp.asImageBitmap()
}

/**
 * Displays regional TMDB Watch Providers ("Where to Watch") and the user's Quick Launch Shortcut row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchProvidersAndShortcutSection(
    dramaId: Int,
    dramaTitle: String,
    watchProviders: List<StreamingProvider>,
    shortcut: Shortcut?,
    accentColor: Color,
    onSaveShortcut: (Shortcut?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current
    val pm = context.packageManager

    var showShortcutSheet by remember { mutableStateOf(false) }

    if (showShortcutSheet) {
        AddShortcutSheet(
            dramaId = dramaId,
            defaultTitle = dramaTitle,
            pm = pm,
            onSave = {
                onSaveShortcut(it)
                showShortcutSheet = false
            },
            onDismiss = { showShortcutSheet = false }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TMDB Regional Watch Providers
        if (watchProviders.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MoglogShapes.Card)
                    .background(colors.secondaryBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_where_to_watch),
                    style = typo.headline,
                    color = colors.label
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(watchProviders, key = { it.id }) { provider ->
                        Row(
                            modifier = Modifier
                                .clip(SmoothRoundedCornerShape(10.dp))
                                .background(colors.tertiaryBackground)
                                .iosClickable(
                                    hapticOnClick = true,
                                    onClick = {
                                        val link = provider.link ?: "https://www.google.com/search?q=${Uri.encode("$dramaTitle ${provider.name}")}"
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                        } catch (_: Exception) {
                                        }
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (provider.logoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(provider.logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = provider.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(SmoothRoundedCornerShape(6.dp))
                                )
                            }
                            Text(
                                text = provider.name,
                                style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Quick Launch Shortcut Card
        IosGroupedSection(header = stringResource(R.string.detail_shortcut_title)) {
            if (shortcut == null) {
                IosGroupedRow(
                    title = stringResource(R.string.detail_add_shortcut),
                    subtitle = "Pin a streaming app or direct episode link for this show",
                    icon = MoglogIcons.Play,
                    iconBackground = accentColor,
                    showChevron = true,
                    showDivider = false,
                    onClick = { showShortcutSheet = true }
                )
            } else {
                IosGroupedRow(
                    title = stringResource(R.string.detail_open_shortcut, shortcut.title),
                    subtitle = if (shortcut.type == "APP") "Streaming App" else shortcut.data,
                    icon = MoglogIcons.ExternalLink,
                    iconBackground = accentColor,
                    showChevron = true,
                    onClick = {
                        try {
                            if (shortcut.type == "APP") {
                                val launchIntent = pm.getLaunchIntentForPackage(shortcut.data)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    val fallback = CURATED_STREAMING_APPS.find { it.packageName == shortcut.data }?.fallbackUrl
                                        ?: "https://www.google.com/search?q=${Uri.encode(shortcut.title)}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
                                }
                            } else {
                                val url = if (shortcut.data.startsWith("http")) shortcut.data else "https://${shortcut.data}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        } catch (_: Exception) {
                        }
                    }
                )
                IosGroupedRow(
                    title = stringResource(R.string.detail_remove_shortcut),
                    titleColor = colors.destructive,
                    icon = MoglogIcons.Trash,
                    iconBackground = colors.destructive,
                    showDivider = false,
                    onClick = { onSaveShortcut(null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutSheet(
    dramaId: Int,
    defaultTitle: String,
    pm: PackageManager,
    onSave: (Shortcut) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    var linkTitle by remember { mutableStateOf(defaultTitle) }
    var linkUrl by remember { mutableStateOf("") }

    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_add_shortcut),
                style = typo.title2,
                color = colors.label
            )

            // Curated Streaming Apps
            IosGroupedSection(header = "Popular Streaming Apps") {
                CURATED_STREAMING_APPS.forEachIndexed { index, app ->
                    val installedIcon = remember(app.packageName) {
                        try {
                            pm.getApplicationIcon(app.packageName)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    IosGroupedRow(
                        title = app.name,
                        subtitle = if (installedIcon != null) "Installed on device" else "Opens web player",
                        icon = if (installedIcon == null) MoglogIcons.Play else null,
                        showChevron = true,
                        showDivider = index < CURATED_STREAMING_APPS.lastIndex,
                        onClick = {
                            onSave(
                                Shortcut(
                                    title = app.name,
                                    data = if (installedIcon != null) app.packageName else app.fallbackUrl,
                                    type = if (installedIcon != null) "APP" else "LINK",
                                    dramaId = dramaId
                                )
                            )
                        },
                        trailing = if (installedIcon != null) {
                            {
                                Image(
                                    bitmap = installedIcon.toComposeImageBitmap(),
                                    contentDescription = app.name,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(SmoothRoundedCornerShape(6.dp))
                                )
                            }
                        } else null
                    )
                }
            }

            // Custom Direct Web Link
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MoglogShapes.Card)
                    .background(colors.tertiaryBackground)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Or Custom Web Link",
                    style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.label
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SmoothRoundedCornerShape(8.dp))
                        .background(colors.secondaryBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (linkTitle.isEmpty()) {
                        Text("Label (e.g. Netflix, Viki)", style = typo.footnote, color = colors.tertiaryLabel)
                    }
                    BasicTextField(
                        value = linkTitle,
                        onValueChange = { linkTitle = it },
                        singleLine = true,
                        textStyle = typo.footnote.copy(color = colors.label),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SmoothRoundedCornerShape(8.dp))
                        .background(colors.secondaryBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (linkUrl.isEmpty()) {
                        Text("https://...", style = typo.footnote, color = colors.tertiaryLabel)
                    }
                    BasicTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        singleLine = true,
                        textStyle = typo.footnote.copy(color = colors.label),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IosPrimaryButton(
                    text = "Save Link Shortcut",
                    enabled = linkUrl.isNotBlank(),
                    onClick = {
                        val rawUrl = linkUrl.trim()
                        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
                        onSave(
                            Shortcut(
                                title = linkTitle.ifBlank { "Watch Link" },
                                data = fullUrl,
                                type = "LINK",
                                dramaId = dramaId
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
