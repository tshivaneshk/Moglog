package com.skech.moglog.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.skech.moglog.R
import com.skech.moglog.ui.designsystem.MoglogIcons
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes using IDs rather than serialized JSON payloads.
 */
object MoglogRoutes {
    @Serializable
    data object Onboarding

    @Serializable
    data object Library

    @Serializable
    data object Discover

    @Serializable
    data object Stats

    @Serializable
    data object Settings

    @Serializable
    data class Detail(val dramaId: Int)

    @Serializable
    data object RecycleBin

    @Serializable
    data object Privacy
}

enum class MainTab(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    LIBRARY(MoglogRoutes.Library, R.string.tab_library, MoglogIcons.Library),
    DISCOVER(MoglogRoutes.Discover, R.string.tab_discover, MoglogIcons.Discover),
    STATS(MoglogRoutes.Stats, R.string.tab_stats, MoglogIcons.Stats),
    SETTINGS(MoglogRoutes.Settings, R.string.tab_settings, MoglogIcons.Settings)
}
