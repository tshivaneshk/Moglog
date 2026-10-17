package com.skech.moglog.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Continuous-corner (squircle-like) shape using smooth cubic bezier curves at corners.
 * Standard radii: 10.dp (controls), 14.dp (cards/posters), 20.dp (sheets/modals).
 */
@Immutable
class SmoothRoundedCornerShape(
    private val cornerRadius: Dp,
    private val smoothness: Float = 0.6f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val maxR = min(size.width, size.height) / 2f
        val radius = r.coerceIn(0f, maxR)
        if (radius <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }

        val w = size.width
        val h = size.height
        val s = (radius * (1f + smoothness * 0.25f)).coerceAtMost(maxR)
        val c = radius * 0.5522847f

        val path = Path().apply {
            moveTo(s, 0f)
            lineTo(w - s, 0f)
            cubicTo(w - s + c, 0f, w, s - c, w, s)
            lineTo(w, h - s)
            cubicTo(w, h - s + c, w - s + c, h, w - s, h)
            lineTo(s, h)
            cubicTo(s - c, h, 0f, h - s + c, 0f, h - s)
            lineTo(0f, s)
            cubicTo(0f, s - c, s - c, 0f, s, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

object MoglogShapes {
    val Control = SmoothRoundedCornerShape(10.dp)
    val Card = SmoothRoundedCornerShape(14.dp)
    val Poster = SmoothRoundedCornerShape(14.dp)
    val Sheet = SmoothRoundedCornerShape(20.dp)
    val TopSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val Pill = RoundedCornerShape(percent = 50)
}
