package app.rewind.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.rewind.R

/**
 * A small video rental store after dark, mid-nineties: a pink neon sign in the window, a cyan
 * "open" under it, blue carpet, black shelves of boxes, a counter with a till, receipts on thin
 * paper. The store only keeps evening hours, so there's only the night look.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** The pink neon: for the one thing to do. */
    val neon: Color,
    val neonSoft: Color,
    val onNeon: Color,
    /** The cyan neon: "open", and what's in hand. */
    val cyan: Color,
    val cyanSoft: Color,
    /** The amber of "late" and of the till's display. */
    val amber: Color,
    /** The black shelves and the carpet under them. */
    val shelf: Color,
    val shelfEdge: Color,
    val carpet: Color,
    /** Receipts, slips and the library card: thin paper, with ballpoint and a stamp on it. */
    val paper: Color,
    val paperLine: Color,
    val pen: Color,
    val stamp: Color,
    /** Brown wrapping paper for the clerk's pick. */
    val kraft: Color,
)

val Night = Palette(
    bg = Color(0xFF0D0B14),
    surface = Color(0xFF17131F),
    surface2 = Color(0xFF211B2C),
    line = Color(0xFF332A42),
    ink = Color(0xFFF4EEE3),
    muted = Color(0xFFB9B0C7),
    faint = Color(0xFF7E7590),
    neon = Color(0xFFFF4FA3),
    neonSoft = Color(0xFF3A1730),
    onNeon = Color(0xFF1A0712),
    cyan = Color(0xFF52E6FF),
    cyanSoft = Color(0xFF12313A),
    amber = Color(0xFFFFB547),
    shelf = Color(0xFF1B1822),
    shelfEdge = Color(0xFF2E2938),
    carpet = Color(0xFF1C2350),
    paper = Color(0xFFF5F0E4),
    paperLine = Color(0xFFDCD3C1),
    pen = Color(0xFF233A86),
    stamp = Color(0xFFC8324B),
    kraft = Color(0xFFB98A5A),
)

val LocalPalette = staticCompositionLocalOf { Night }

/** Russo One: the chunky lettering on a VHS box and the store's price tags. */
val Block = FontFamily(Font(R.font.russo_one, FontWeight.Normal))

/** Lobster: bent glass tubes, for the neon sign. */
val Neon = FontFamily(Font(R.font.lobster, FontWeight.Normal))

/** Manrope: plain and clear, for everything else. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Caveat: ballpoint on a return slip or the back of a box. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

/** PT Mono: the till's receipts and the counter on the VCR. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

object UType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Block, fontSize = 30.sp, lineHeight = 36.sp, color = U.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Block, fontSize = 23.sp, lineHeight = 29.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Block, fontSize = 18.sp, lineHeight = 24.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 12.sp, letterSpacing = 0.16.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = U.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val Note: TextStyle @Composable get() = TextStyle(fontFamily = Hand, fontSize = 24.sp, lineHeight = 27.sp, color = U.c.pen)
    val Receipt: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.04.em, color = Color(0xFF2A2630))

    /** Glowing tubes: the colour, with its own light around it. */
    fun glow(color: Color, size: Float = 40f, blur: Float = 24f) =
        TextStyle(fontFamily = Neon, fontSize = size.sp, color = color, shadow = Shadow(color.copy(alpha = 0.9f), blurRadius = blur))
}

@Composable
fun RewindTheme(content: @Composable () -> Unit) {
    val p = Night
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = darkColorScheme().copy(
                primary = p.neon,
                onPrimary = p.onNeon,
                primaryContainer = p.neonSoft,
                onPrimaryContainer = p.ink,
                secondary = p.cyan,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface2,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 15.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
