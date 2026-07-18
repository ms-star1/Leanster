package com.bike.leanster

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.bike.leanster.ui.theme.NeonCyan
import kotlin.math.tan

/**
 * The Leanster logo — brand [mark][R.drawable.ic_leanster_mark] plus the "Lean·ster" wordmark.
 *
 * Reproduces the export variants from the "Leanster Logo Exports" design sheet:
 * two [orientations][LeansterLogoOrientation] (horizontal / stacked) across the
 * [dark][LeansterLogoScheme.Dark] and [light][LeansterLogoScheme.Light] colour schemes.
 * Backgrounds (solid vs. transparent) are left to the caller.
 */
enum class LeansterLogoOrientation { Horizontal, Stacked }

/**
 * Colour scheme for the logo. [mark] and [lean] are the accent (drawn together);
 * [ster] is the muted trailing part of the wordmark.
 */
@Immutable
data class LeansterLogoScheme(
    val mark: Color,
    val lean: Color,
    val ster: Color,
) {
    companion object {
        /** Cyan mark on dark backgrounds. */
        val Dark = LeansterLogoScheme(mark = NeonCyan, lean = NeonCyan, ster = Color(0xFF7A827A))
        /** Ink mark on light backgrounds. */
        val Light = LeansterLogoScheme(mark = Color(0xFF070908), lean = Color(0xFF070908), ster = Color(0xFF8A908A))
    }
}

@Composable
fun LeansterLogo(
    modifier: Modifier = Modifier,
    orientation: LeansterLogoOrientation = LeansterLogoOrientation.Horizontal,
    scheme: LeansterLogoScheme = LeansterLogoScheme.Dark,
    markSize: Dp = if (orientation == LeansterLogoOrientation.Horizontal) 56.dp else 80.dp,
) {
    val horizontal = orientation == LeansterLogoOrientation.Horizontal
    // Wordmark scale relative to the mark, matching the export sheet proportions.
    val wordmarkSize: TextUnit = (markSize.value * if (horizontal) 0.66f else 0.32f).sp

    val mark = @Composable {
        Image(
            painter = painterResource(R.drawable.ic_leanster_mark),
            contentDescription = "Leanster",
            colorFilter = ColorFilter.tint(scheme.mark),
            modifier = Modifier.size(markSize),
        )
    }
    val wordmark = @Composable { Wordmark(scheme, wordmarkSize) }

    if (horizontal) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(markSize.value.times(0.3f).dp),
        ) {
            mark()
            wordmark()
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(markSize.value.times(0.22f).dp),
        ) {
            mark()
            wordmark()
        }
    }
}

@Composable
private fun Wordmark(scheme: LeansterLogoScheme, fontSize: TextUnit) {
    // Slight negative tracking, matching the export sheet (letter-spacing: -.03em).
    val tracking = (fontSize.value * -0.03f).sp
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "Lean",
            color = scheme.lean,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            style = TextStyle(letterSpacing = tracking),
            // -12deg shear, as in the design.
            modifier = Modifier
                .alignByBaseline()
                .drawWithContent {
                    val skew = tan(Math.toRadians(-12.0)).toFloat()
                    val canvas = drawContext.canvas.nativeCanvas
                    val save = canvas.save()
                    // Shear about the vertical centre so the glyphs stay put.
                    canvas.translate(-skew * size.height / 2f, 0f)
                    canvas.skew(skew, 0f)
                    drawContent()
                    canvas.restoreToCount(save)
                },
        )
        Text(
            text = "ster",
            color = scheme.ster,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            style = TextStyle(letterSpacing = tracking),
            modifier = Modifier.alignByBaseline(),
        )
    }
}
