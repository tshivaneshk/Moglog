package com.skech.moglog.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skech.moglog.ui.designsystem.IosCircleIconButton
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosLargeTitleHeader
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons

@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IosLargeTitleHeader(
            title = "Privacy & Security",
            collapsed = true,
            leadingActions = {
                IosCircleIconButton(
                    icon = MoglogIcons.ChevronLeft,
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text(
                        text = "Built for Local-First Privacy",
                        style = typo.title2,
                        color = colors.label
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Moglog stores your library, notes, and watch progress on your device inside an encrypted SQLCipher database backed by Android Keystore.",
                        style = typo.subhead,
                        color = colors.secondaryLabel
                    )
                }
            }

            item {
                IosGroupedSection(header = "On-Device Encryption") {
                    PrivacyBulletRow(
                        title = "Encrypted Local Database",
                        body = "Your watchlist, personal ratings, and notes are encrypted at rest with SQLCipher (256-bit AES) using a random passphrase sealed in Android Keystore via Google Tink."
                    )
                    PrivacyBulletRow(
                        title = "Protected API Credentials",
                        body = "Your personal TMDB API key or Read Access Token is encrypted with AES-256-GCM in SecureStore and is never synced to Firestore or third-party servers."
                    )
                    PrivacyBulletRow(
                        title = "Excluded from Auto-Backup",
                        body = "Android cloud backup and device-to-device extraction rules explicitly exclude encrypted databases and keystore keysets so keys never leave your hardware."
                    )
                }
            }

            item {
                IosGroupedSection(header = "Optional Cloud Sync & E2E Backup") {
                    PrivacyBulletRow(
                        title = "Zero-Knowledge Passphrase Option",
                        body = "When End-to-End Encrypted Cloud Backup is enabled, your library is encrypted client-side using PBKDF2-HMAC-SHA256 (600,000 iterations) + AES-256-GCM before upload."
                    )
                    PrivacyBulletRow(
                        title = "Full Account Control",
                        body = "You can export your library to an encrypted .moglog archive or permanently delete your cloud backup and Firebase Auth account at any time from Settings."
                    )
                }
            }

            item {
                IosGroupedSection(
                    header = "Third-Party Services",
                    footer = "This product uses the TMDB API but is not endorsed or certified by TMDB."
                ) {
                    IosGroupedRow(
                        title = "The Movie Database (TMDB)",
                        subtitle = "Used directly from your device to fetch show posters, episode counts, and regional streaming availability.",
                        showDivider = true
                    )
                    IosGroupedRow(
                        title = "Google Firebase (Optional)",
                        subtitle = "Used only if you sign in with Google for cross-device cloud backup.",
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyBulletRow(
    title: String,
    body: String
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = typo.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = typo.footnote,
            color = colors.secondaryLabel
        )
    }
}
