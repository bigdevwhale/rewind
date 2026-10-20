package app.rewind.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.rewind.data.Kind
import app.rewind.data.Tape
import app.rewind.ui.theme.Block
import app.rewind.ui.theme.Mono
import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/** Width over height of each kind's case: a VHS box, a box set, an audio cassette, a library book. */
fun Kind.aspect(): Float = when (this) {
    Kind.FILM -> 0.57f
    Kind.SERIES -> 0.66f
    Kind.ALBUM -> 0.64f
    Kind.BOOK -> 0.68f
    Kind.COMIC -> 0.66f
}

/**
 * A tape face out on the shelf, the way rental stores showed them. With a [cover] from the
 * catalogue it's printed on the box; without one the box is drawn, in nineties two-colour printing.
 * [fade] bleaches it as it waits untouched in the window's sun; [wear] scuffs the corners and creases
 * it as it's taken home again and again. [sticker] is the store's round price-gun label.
 */
@Composable
fun TapeBox(tape: Tape, cover: ImageBitmap?, modifier: Modifier = Modifier, width: Dp = 104.dp, fade: Float = 0f, wear: Float = 0f, sticker: String? = null) {
    val kind = tape.kindOf
    val scheme = Art.scheme(tape.seed)
    val shape = RoundedCornerShape(if (kind == Kind.ALBUM) 5.dp else 3.dp)
    Box(modifier.width(width).aspectRatio(kind.aspect())) {
        BoxWithConstraints(Modifier.fillMaxSize().shadow(6.dp, shape).clip(shape).background(scheme.ground)) {
            val w = maxWidth
            when (kind) {
                Kind.FILM -> Vhs(tape, cover, scheme, w, fade)
                Kind.SERIES -> BoxSet(tape, cover, scheme, w, fade)
                Kind.ALBUM -> Cassette(tape, cover, scheme, w, fade)
                Kind.BOOK -> Book(tape, cover, scheme, w, fade)
                Kind.COMIC -> Comic(tape, cover, scheme, w, fade)
            }
            // Sun: the colours wash out toward a pale blue-white.
            if (fade > 0f) Box(Modifier.fillMaxSize().background(Color(0xFFE4EEEC).copy(alpha = 0.55f * fade)))
            if (wear > 0f) Canvas(Modifier.fillMaxSize()) { scuffs(tape.seed, wear) }
        }
        if (sticker != null) Box(
            Modifier.align(Alignment.TopEnd).padding(top = width * 0.04f).width(width * 0.36f).aspectRatio(1f).rotate(12f)
                .shadow(2.dp, CircleShape).clip(CircleShape).background(Color(0xFFFFD23F)),
            contentAlignment = Alignment.Center,
        ) {
            Text(sticker, style = TextStyle(fontFamily = Block, fontSize = (width.value * 0.085f).sp, color = Color(0xFF1A1206), textAlign = TextAlign.Center, lineHeight = (width.value * 0.09f).sp), maxLines = 2)
        }
    }
}

private fun saturation(fade: Float) = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f - 0.75f * fade) })

@Composable
private fun Picture(cover: ImageBitmap, fade: Float, modifier: Modifier = Modifier) {
    Image(cover, null, modifier, contentScale = ContentScale.Crop, colorFilter = if (fade > 0f) saturation(fade) else null)
}

/** The title on a drawn box, as big as the box allows. */
@Composable
private fun BoxTitle(text: String, color: Color, w: Dp, modifier: Modifier = Modifier, scale: Float = 1f, lines: Int = 4, align: TextAlign = TextAlign.Start) {
    val long = text.length > 18
    // Small enough that the longest word fits on a line: block capitals are about 0.85 em wide.
    val word = text.split(' ', '-').maxOf { it.length }.coerceAtLeast(1)
    val fit = 0.86f / (word * 0.85f)
    val size = minOf(if (long) 0.12f else 0.155f, fit) * scale
    Text(
        text.uppercase(),
        modifier = modifier,
        style = TextStyle(fontFamily = Block, color = color, fontSize = (w.value * size).sp, lineHeight = (w.value * size * 1.12f).sp, letterSpacing = 0.01.em, textAlign = align),
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Small(text: String, color: Color, w: Dp, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = TextStyle(fontFamily = Mono, color = color, fontSize = (w.value * 0.075f).sp, letterSpacing = 0.06.em), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A film: a VHS sleeve, the picture with a black band and the format's logo. */
@Composable
private fun Vhs(tape: Tape, cover: ImageBitmap?, s: Art.Scheme, w: Dp, fade: Float) {
    if (cover != null) {
        Column(Modifier.fillMaxSize()) {
            Picture(cover, fade, Modifier.fillMaxWidth().weight(1f))
            Box(Modifier.fillMaxWidth().background(Color(0xFF0E0E10)).padding(horizontal = w * 0.06f, vertical = w * 0.035f)) {
                Small("VHS · HI-FI", Color(0xFFE9E4D8), w)
            }
        }
        return
    }
    Drawn(tape, s, w)
    Box(Modifier.fillMaxSize().padding(w * 0.06f)) {
        Small("VHS", s.ink.copy(alpha = 0.85f), w, Modifier.align(Alignment.TopStart))
    }
}

/** A series: a fatter box set, the tapes' edges showing down the left side. */
@Composable
private fun BoxSet(tape: Tape, cover: ImageBitmap?, s: Art.Scheme, w: Dp, fade: Float) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(start = w * 0.13f)) {
            if (cover != null) Picture(cover, fade, Modifier.fillMaxSize()) else Drawn(tape, s, w * 0.87f)
        }
        Canvas(Modifier.fillMaxSize()) {
            val band = size.width * 0.13f
            drawRect(Color(0xFF101012), size = Size(band, size.height))
            val n = 5
            val gap = size.height * 0.012f
            val h = (size.height - gap * (n + 1)) / n
            repeat(n) { i ->
                val y = gap + i * (h + gap)
                drawRect(s.band, Offset(band * 0.18f, y), Size(band * 0.64f, h))
                drawRect(Color.White.copy(alpha = 0.35f), Offset(band * 0.3f, y + h * 0.4f), Size(band * 0.4f, h * 0.2f))
            }
        }
    }
}

/** An album: an audio cassette in its clear case, the J-card behind the plastic and the hinge on the left. */
@Composable
private fun Cassette(tape: Tape, cover: ImageBitmap?, s: Art.Scheme, w: Dp, fade: Float) {
    Box(Modifier.fillMaxSize().background(Color(0xFFD9DCDF))) {
        Box(Modifier.fillMaxSize().padding(start = w * 0.1f, top = w * 0.04f, end = w * 0.04f, bottom = w * 0.04f).clip(RoundedCornerShape(2.dp))) {
            if (cover != null) {
                Column(Modifier.fillMaxSize()) {
                    Picture(cover, fade, Modifier.fillMaxWidth().aspectRatio(1f))
                    Column(Modifier.fillMaxSize().background(s.ground).padding(horizontal = w * 0.05f, vertical = w * 0.03f)) {
                        Small(tape.creator.ifBlank { tape.title }, s.ink, w)
                        Small(if (tape.creator.isBlank()) "" else tape.title, s.ink.copy(alpha = 0.75f), w)
                    }
                }
            } else Drawn(tape, s, w * 0.86f)
        }
        Canvas(Modifier.fillMaxSize()) {
            // The hinge and its ridges, and light on the plastic.
            val hinge = size.width * 0.1f
            drawRect(Color(0xFFBFC3C8), size = Size(hinge, size.height))
            val step = size.height / 18f
            repeat(17) { i -> drawLine(Color(0xFF9EA3A9), Offset(hinge * 0.25f, step * (i + 1)), Offset(hinge * 0.75f, step * (i + 1)), strokeWidth = 1.2f) }
            drawPath(Path().apply {
                moveTo(size.width * 0.55f, 0f); lineTo(size.width * 0.75f, 0f); lineTo(size.width * 0.3f, size.height); lineTo(size.width * 0.1f, size.height); close()
            }, Color.White.copy(alpha = 0.12f))
        }
    }
}

/** A book: library cloth, a darker spine, the title stamped, and the card pocket's edge. */
@Composable
private fun Book(tape: Tape, cover: ImageBitmap?, s: Art.Scheme, w: Dp, fade: Float) {
    Box(Modifier.fillMaxSize()) {
        if (cover != null) Picture(cover, fade, Modifier.fillMaxSize().padding(start = w * 0.08f))
        else {
            Box(Modifier.fillMaxSize().background(s.ground))
            Canvas(Modifier.fillMaxSize()) {
                // Cloth weave.
                val step = 3.dp.toPx()
                var y = 0f
                while (y < size.height) { drawLine(Color.Black.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
                val frame = size.width * 0.1f
                drawRect(s.band, Offset(size.width * 0.16f, frame), Size(size.width * 0.76f, size.height - frame * 2), style = Stroke(1.5.dp.toPx()))
            }
            Column(Modifier.fillMaxSize().padding(start = w * 0.24f, end = w * 0.12f, top = w * 0.22f), horizontalAlignment = Alignment.CenterHorizontally) {
                BoxTitle(tape.title, s.band, w * 0.85f, align = TextAlign.Center, scale = 0.9f)
                if (tape.creator.isNotBlank()) Small(tape.creator, s.band.copy(alpha = 0.85f), w, Modifier.padding(top = w * 0.04f))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val spine = size.width * 0.08f
            drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.2f)), endX = spine), size = Size(spine, size.height))
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(spine * 1.4f, 0f), Offset(spine * 1.4f, size.height), 1.2f)
        }
    }
}

/**
 * A comic: a newsstand issue, the masthead across the top, the price box in the corner, and on a
 * drawn one, halftone dots and a burst behind the title.
 */
@Composable
private fun Comic(tape: Tape, cover: ImageBitmap?, s: Art.Scheme, w: Dp, fade: Float) {
    Box(Modifier.fillMaxSize()) {
        if (cover != null) Picture(cover, fade, Modifier.fillMaxSize())
        else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(s.band)
                // Halftone: dots growing toward the bottom.
                val step = size.width / 14f
                var y = step / 2
                var row = 0
                while (y < size.height) {
                    var x = if (row % 2 == 0) step / 2 else step
                    val r = step * 0.12f + step * 0.3f * (y / size.height)
                    while (x < size.width) { drawCircle(s.accent.copy(alpha = 0.55f), r, Offset(x, y)); x += step }
                    y += step * 0.87f
                    row++
                }
                // A burst behind the title.
                val c = Offset(size.width * 0.5f, size.height * 0.62f)
                val burst = Path()
                repeat(24) { k ->
                    val a = k * PI.toFloat() / 12
                    val rr = if (k % 2 == 0) size.width * 0.48f else size.width * 0.3f
                    if (k == 0) burst.moveTo(c.x + cos(a) * rr, c.y + sin(a) * rr) else burst.lineTo(c.x + cos(a) * rr, c.y + sin(a) * rr)
                }
                burst.close()
                drawPath(burst, s.ink)
                drawPath(burst, Color(0xFF111111), style = Stroke(2.dp.toPx()))
            }
            Box(Modifier.align(Alignment.Center).padding(top = w * 0.32f, start = w * 0.14f, end = w * 0.14f)) {
                BoxTitle(tape.title, Color(0xFF111111), w * 0.8f, align = TextAlign.Center, lines = 3)
            }
        }
        // Masthead and the price box.
        Box(Modifier.fillMaxWidth().background(if (cover != null) Color(0xFF111111).copy(alpha = 0.78f) else s.ground).padding(start = w * 0.24f, end = w * 0.05f, top = w * 0.03f, bottom = w * 0.03f)) {
            Small(tape.creator.ifBlank { "COMICS" }.uppercase(), if (cover != null) Color.White else s.ink, w)
        }
        Column(Modifier.width(w * 0.2f).background(Color.White).padding(vertical = w * 0.015f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("№" + (1 + (tape.seed.mod(40L))).toString(), style = TextStyle(fontFamily = Block, fontSize = (w.value * 0.07f).sp, color = Color(0xFF111111)))
            Text("25¢", style = TextStyle(fontFamily = Mono, fontSize = (w.value * 0.05f).sp, color = Color(0xFF111111)))
        }
    }
}

/** A box with no picture: one of four nineties layouts, the title in block capitals. */
@Composable
private fun Drawn(tape: Tape, s: Art.Scheme, w: Dp) {
    val layout = Art.layout(tape.seed)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val W = size.width
            val H = size.height
            when (layout) {
                // Diagonal speed stripes across the top.
                0 -> clipRect {
                    repeat(4) { i ->
                        val y = H * (0.08f + i * 0.07f)
                        drawPath(Path().apply { moveTo(W * 0.25f, y + H * 0.12f); lineTo(W * 1.1f, y - H * 0.08f); lineTo(W * 1.1f, y - H * 0.04f); lineTo(W * 0.25f, y + H * 0.16f); close() }, if (i % 2 == 0) s.band else s.accent)
                    }
                }
                // A sunset over a grid floor.
                1 -> {
                    drawCircle(Brush.verticalGradient(listOf(s.band, s.accent), startY = H * 0.12f, endY = H * 0.5f), W * 0.32f, Offset(W * 0.5f, H * 0.33f))
                    repeat(4) { i -> drawRect(s.ground, Offset(0f, H * (0.36f + i * 0.04f)), Size(W, H * 0.012f * (i + 1))) }
                    drawRect(s.ground, Offset(0f, H * 0.5f), Size(W, H * 0.5f))
                    repeat(6) { i -> drawLine(s.accent.copy(alpha = 0.55f), Offset(0f, H * (0.52f + i * i * 0.012f)), Offset(W, H * (0.52f + i * i * 0.012f)), 1.5f) }
                    repeat(7) { i -> drawLine(s.accent.copy(alpha = 0.55f), Offset(W * 0.5f, H * 0.5f), Offset(W * (i / 6f) * 2f - W * 0.5f, H), 1.5f) }
                }
                // A thick band with a thin rule, across the middle.
                2 -> {
                    drawRect(s.band, Offset(0f, H * 0.18f), Size(W, H * 0.26f))
                    drawRect(s.accent, Offset(0f, H * 0.46f), Size(W, H * 0.02f))
                    val rnd = Random(tape.seed)
                    repeat(14) { drawCircle(s.ink.copy(alpha = 0.6f), 1.2f + rnd.nextFloat() * 1.5f, Offset(rnd.nextFloat() * W, H * 0.55f + rnd.nextFloat() * H * 0.12f)) }
                }
                // Vertical bars of colour, like a test card.
                else -> {
                    val bars = listOf(s.band, s.accent, s.ink, s.band.copy(alpha = 0.6f), s.accent.copy(alpha = 0.6f))
                    bars.forEachIndexed { i, c -> drawRect(c, Offset(W * i / bars.size, H * 0.1f), Size(W / bars.size + 1, H * 0.36f)) }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(w * 0.07f)) {
            BoxTitle(tape.title, s.ink, w)
            if (tape.creator.isNotBlank()) Small(tape.creator, s.ink.copy(alpha = 0.75f), w, Modifier.padding(top = w * 0.03f))
        }
    }
}

/** Soft white corners, a crease, a strip of tape on the edge: a box that has been home many times. */
private fun DrawScope.scuffs(seed: Long, wear: Float) {
    val rnd = Random(seed * 7 + 3)
    val white = Color.White.copy(alpha = 0.55f * wear)
    val r = size.minDimension * (0.05f + 0.06f * wear)
    listOf(Offset(0f, 0f), Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height)).forEach { c ->
        if (rnd.nextFloat() < 0.4f + wear * 0.6f) drawCircle(white, r * (0.6f + rnd.nextFloat()), c)
    }
    if (wear > 0.3f) {
        val y = size.height * (0.3f + rnd.nextFloat() * 0.4f)
        drawLine(Color.White.copy(alpha = 0.35f * wear), Offset(0f, y), Offset(size.width, y + size.height * (rnd.nextFloat() - 0.5f) * 0.2f), 1.5f, cap = StrokeCap.Round)
    }
    if (wear > 0.6f) {
        drawRoundRect(Color(0xFFF2EBD3).copy(alpha = 0.7f), Offset(size.width * 0.3f, -size.height * 0.01f), Size(size.width * 0.3f, size.height * 0.05f), CornerRadius(2f))
    }
}
