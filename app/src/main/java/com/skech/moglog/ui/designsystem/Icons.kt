package com.skech.moglog.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Clean, rounded-outline 24dp vector icon set inspired by Lucide / Phosphor.
 * Replaces Material filled icons and emoji stars across the app.
 */
object MoglogIcons {

    private fun outlineIcon(
        name: String,
        strokeWidth: Float = 2f,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()
    }

    private fun ImageVector.Builder.strokePath(
        strokeWidth: Float = 2f,
        pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = pathBuilder
        )
    }

    val Library: ImageVector by lazy {
        outlineIcon("Library") {
            strokePath {
                // Rounded rectangle stack (TV/Library cards)
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                moveTo(6f, 3f)
                lineTo(18f, 3f)
                moveTo(5f, 9f)
                horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 21f, 11f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f)
                verticalLineTo(11f)
                arcTo(2f, 2f, 0f, false, true, 5f, 9f)
                close()
                // Play triangle inside
                moveTo(10f, 12.5f)
                lineTo(15f, 15f)
                lineTo(10f, 17.5f)
                close()
            }
        }
    }

    val Discover: ImageVector by lazy {
        outlineIcon("Discover") {
            strokePath {
                // Compass circle
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                close()
                // Compass diamond needle
                moveTo(16.24f, 7.76f)
                lineTo(14.12f, 14.12f)
                lineTo(7.76f, 16.24f)
                lineTo(9.88f, 9.88f)
                close()
            }
        }
    }

    val Stats: ImageVector by lazy {
        outlineIcon("Stats") {
            strokePath {
                moveTo(18f, 20f)
                verticalLineTo(10f)
                moveTo(12f, 20f)
                verticalLineTo(4f)
                moveTo(6f, 20f)
                verticalLineTo(14f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        outlineIcon("Settings") {
            strokePath {
                // Sliders / gear hybrid clean lines
                moveTo(4f, 21f)
                verticalLineTo(14f)
                moveTo(4f, 10f)
                verticalLineTo(3f)
                moveTo(12f, 21f)
                verticalLineTo(12f)
                moveTo(12f, 8f)
                verticalLineTo(3f)
                moveTo(20f, 21f)
                verticalLineTo(16f)
                moveTo(20f, 12f)
                verticalLineTo(3f)
                moveTo(1f, 14f)
                horizontalLineTo(7f)
                moveTo(9f, 8f)
                horizontalLineTo(15f)
                moveTo(17f, 16f)
                horizontalLineTo(23f)
            }
        }
    }

    val Search: ImageVector by lazy {
        outlineIcon("Search") {
            strokePath {
                moveTo(19f, 11f)
                arcTo(8f, 8f, 0f, true, true, 3f, 11f)
                arcTo(8f, 8f, 0f, true, true, 19f, 11f)
                close()
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        outlineIcon("Plus") {
            strokePath {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }

    val Minus: ImageVector by lazy {
        outlineIcon("Minus") {
            strokePath {
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }

    val Check: ImageVector by lazy {
        outlineIcon("Check", strokeWidth = 2.2f) {
            strokePath(strokeWidth = 2.2f) {
                moveTo(20f, 6f)
                lineTo(9f, 17f)
                lineTo(4f, 12f)
            }
        }
    }

    val Close: ImageVector by lazy {
        outlineIcon("Close") {
            strokePath {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }
    }

    val ChevronLeft: ImageVector by lazy {
        outlineIcon("ChevronLeft", strokeWidth = 2.2f) {
            strokePath(strokeWidth = 2.2f) {
                moveTo(15f, 18f)
                lineTo(9f, 12f)
                lineTo(15f, 6f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        outlineIcon("ChevronRight", strokeWidth = 2f) {
            strokePath(strokeWidth = 2f) {
                moveTo(9f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 6f)
            }
        }
    }

    val MoreHorizontal: ImageVector by lazy {
        outlineIcon("MoreHorizontal") {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 12f)
                moveToRelative(-1.6f, 0f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 3.2f, 0f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, -3.2f, 0f)
                moveTo(19f, 12f)
                moveToRelative(-1.6f, 0f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 3.2f, 0f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, -3.2f, 0f)
                moveTo(5f, 12f)
                moveToRelative(-1.6f, 0f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 3.2f, 0f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, -3.2f, 0f)
            }
        }
    }

    val StarFilled: ImageVector by lazy {
        outlineIcon("StarFilled") {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(14.9f, 8.4f)
                lineTo(21.4f, 9.3f)
                lineTo(16.7f, 13.9f)
                lineTo(17.8f, 20.4f)
                lineTo(12f, 17.3f)
                lineTo(6.2f, 20.4f)
                lineTo(7.3f, 13.9f)
                lineTo(2.6f, 9.3f)
                lineTo(9.1f, 8.4f)
                close()
            }
        }
    }

    val StarOutline: ImageVector by lazy {
        outlineIcon("StarOutline") {
            strokePath {
                moveTo(12f, 2.5f)
                lineTo(14.9f, 8.4f)
                lineTo(21.4f, 9.3f)
                lineTo(16.7f, 13.9f)
                lineTo(17.8f, 20.4f)
                lineTo(12f, 17.3f)
                lineTo(6.2f, 20.4f)
                lineTo(7.3f, 13.9f)
                lineTo(2.6f, 9.3f)
                lineTo(9.1f, 8.4f)
                close()
            }
        }
    }

    val Filter: ImageVector by lazy {
        outlineIcon("Filter") {
            strokePath {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                moveTo(7f, 12f)
                horizontalLineTo(17f)
                moveTo(10f, 18f)
                horizontalLineTo(14f)
            }
        }
    }

    val Grid: ImageVector by lazy {
        outlineIcon("Grid") {
            strokePath {
                moveTo(3f, 3f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(3f)
                close()
                moveTo(14f, 3f)
                horizontalLineTo(21f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
                close()
                moveTo(14f, 14f)
                horizontalLineTo(21f)
                verticalLineTo(21f)
                horizontalLineTo(14f)
                close()
                moveTo(3f, 14f)
                horizontalLineTo(10f)
                verticalLineTo(21f)
                horizontalLineTo(3f)
                close()
            }
        }
    }

    val ListRows: ImageVector by lazy {
        outlineIcon("ListRows") {
            strokePath {
                moveTo(8f, 6f)
                horizontalLineTo(21f)
                moveTo(8f, 12f)
                horizontalLineTo(21f)
                moveTo(8f, 18f)
                horizontalLineTo(21f)
                moveTo(3f, 6f)
                horizontalLineTo(3.01f)
                moveTo(3f, 12f)
                horizontalLineTo(3.01f)
                moveTo(3f, 18f)
                horizontalLineTo(3.01f)
            }
        }
    }

    val Trash: ImageVector by lazy {
        outlineIcon("Trash") {
            strokePath {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                moveTo(19f, 6f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, false, true, 17f, 22f)
                horizontalLineTo(7f)
                arcTo(2f, 2f, 0f, false, true, 5f, 20f)
                verticalLineTo(6f)
                moveTo(8f, 6f)
                verticalLineTo(4f)
                arcTo(2f, 2f, 0f, false, true, 10f, 2f)
                horizontalLineTo(14f)
                arcTo(2f, 2f, 0f, false, true, 16f, 4f)
                verticalLineTo(6f)
            }
        }
    }

    val Share: ImageVector by lazy {
        outlineIcon("Share") {
            strokePath {
                moveTo(4f, 12f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, false, false, 6f, 22f)
                horizontalLineTo(18f)
                arcTo(2f, 2f, 0f, false, false, 20f, 20f)
                verticalLineTo(12f)
                moveTo(16f, 6f)
                lineTo(12f, 2f)
                lineTo(8f, 6f)
                moveTo(12f, 2f)
                verticalLineTo(15f)
            }
        }
    }

    val Play: ImageVector by lazy {
        outlineIcon("Play") {
            strokePath {
                moveTo(5f, 3f)
                lineTo(19f, 12f)
                lineTo(5f, 21f)
                close()
            }
        }
    }

    val ExternalLink: ImageVector by lazy {
        outlineIcon("ExternalLink") {
            strokePath {
                moveTo(18f, 13f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 16f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f)
                verticalLineTo(8f)
                arcTo(2f, 2f, 0f, false, true, 5f, 6f)
                horizontalLineTo(11f)
                moveTo(15f, 3f)
                horizontalLineTo(21f)
                verticalLineTo(9f)
                moveTo(10f, 14f)
                lineTo(21f, 3f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        outlineIcon("Lock") {
            strokePath {
                moveTo(5f, 11f)
                horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 21f, 13f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, false, true, 19f, 22f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 20f)
                verticalLineTo(13f)
                arcTo(2f, 2f, 0f, false, true, 5f, 11f)
                close()
                moveTo(7f, 11f)
                verticalLineTo(7f)
                arcTo(5f, 5f, 0f, false, true, 17f, 7f)
                verticalLineTo(11f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        outlineIcon("Shield") {
            strokePath {
                moveTo(12f, 22f)
                curveTo(12f, 22f, 20f, 18f, 20f, 12f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(12f)
                curveTo(4f, 18f, 12f, 22f, 12f, 22f)
                close()
            }
        }
    }

    val Cloud: ImageVector by lazy {
        outlineIcon("Cloud") {
            strokePath {
                moveTo(18f, 10f)
                horizontalLineTo(16.74f)
                arcTo(8f, 8f, 0f, true, false, 9f, 20f)
                horizontalLineTo(18f)
                arcTo(5f, 5f, 0f, false, false, 18f, 10f)
                close()
            }
        }
    }

    val Key: ImageVector by lazy {
        outlineIcon("Key") {
            strokePath {
                moveTo(21f, 2f)
                lineTo(19f, 4f)
                moveTo(11.39f, 11.61f)
                arcTo(5.5f, 5.5f, 0f, true, true, 3.61f, 19.39f)
                arcTo(5.5f, 5.5f, 0f, false, true, 11.39f, 11.61f)
                close()
                moveTo(11.39f, 11.61f)
                lineTo(21f, 2f)
                moveTo(15.5f, 7.5f)
                lineTo(18.5f, 10.5f)
            }
        }
    }

    val Note: ImageVector by lazy {
        outlineIcon("Note") {
            strokePath {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                arcTo(2f, 2f, 0f, false, false, 4f, 4f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, false, false, 6f, 22f)
                horizontalLineTo(18f)
                arcTo(2f, 2f, 0f, false, false, 20f, 20f)
                verticalLineTo(8f)
                close()
                moveTo(14f, 2f)
                verticalLineTo(8f)
                horizontalLineTo(20f)
                moveTo(16f, 13f)
                horizontalLineTo(8f)
                moveTo(16f, 17f)
                horizontalLineTo(8f)
                moveTo(10f, 9f)
                horizontalLineTo(8f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        outlineIcon("Refresh") {
            strokePath {
                moveTo(23f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(17f)
                moveTo(1f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(7f)
                moveTo(3.51f, 9f)
                arcTo(9f, 9f, 0f, false, true, 18.36f, 5.64f)
                lineTo(23f, 10f)
                moveTo(1f, 14f)
                lineTo(5.64f, 18.36f)
                arcTo(9f, 9f, 0f, false, false, 20.49f, 15f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        outlineIcon("Bell") {
            strokePath {
                moveTo(18f, 8f)
                arcTo(6f, 6f, 0f, false, false, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                horizontalLineTo(21f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                close()
                moveTo(13.73f, 21f)
                arcTo(2f, 2f, 0f, false, true, 10.27f, 21f)
            }
        }
    }

    val Person: ImageVector by lazy {
        outlineIcon("Person") {
            strokePath {
                moveTo(20f, 21f)
                verticalLineTo(19f)
                arcTo(4f, 4f, 0f, false, false, 16f, 15f)
                horizontalLineTo(8f)
                arcTo(4f, 4f, 0f, false, false, 4f, 19f)
                verticalLineTo(21f)
                moveTo(16f, 7f)
                arcTo(4f, 4f, 0f, true, true, 8f, 7f)
                arcTo(4f, 4f, 0f, true, true, 16f, 7f)
            }
        }
    }

    val SunMoon: ImageVector by lazy {
        outlineIcon("SunMoon") {
            strokePath {
                moveTo(21f, 12.79f)
                arcTo(9f, 9f, 0f, true, true, 11.21f, 3f)
                arcTo(7f, 7f, 0f, false, false, 21f, 12.79f)
                close()
            }
        }
    }

    val Download: ImageVector by lazy {
        outlineIcon("Download") {
            strokePath {
                moveTo(21f, 15f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f)
                verticalLineTo(15f)
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
                moveTo(12f, 15f)
                verticalLineTo(3f)
            }
        }
    }

    val Upload: ImageVector by lazy {
        outlineIcon("Upload") {
            strokePath {
                moveTo(21f, 15f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f)
                verticalLineTo(15f)
                moveTo(17f, 8f)
                lineTo(12f, 3f)
                lineTo(7f, 8f)
                moveTo(12f, 3f)
                verticalLineTo(15f)
            }
        }
    }
}
