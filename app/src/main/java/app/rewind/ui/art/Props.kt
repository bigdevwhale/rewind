package app.rewind.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import app.rewind.ui.theme.Block
import app.rewind.ui.theme.Hand
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The store's sign: [word] in bent pink tubes, [open] in cyan under it, or "closed" dimmed. Now and
 * then a tube stutters, as old neon does.
 */
@Composable
fun NeonSign(word: String, sub: String, lit: Boolean, modifier: Modifier = Modifier) {
    val c = U.c
    val flicker = if (Art.still || !lit) 1f else {
        val t by rememberInfiniteTransition(label = "neon").animateFloat(0f, 1f, infiniteRepeatable(tween(7_000, easing = LinearEasing)), label = "t")
        // Two quick stutters near the end of each cycle.
        if (t in 0.90f..0.91f || t in 0.93f..0.935f) 0.35f else 1f
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(word, style = UType.glow(if (lit) c.neon else c.neon.copy(alpha = 0.35f), 54f, if (lit) 28f else 0f), modifier = Modifier.alpha(flicker))
        Text(sub, style = UType.glow(if (lit) c.cyan else c.faint, 20f, if (lit) 16f else 0f).copy(fontFamily = Block), textAlign = TextAlign.Center)
    }
}

/**
 * The cassette's two reels, seen through its window, for rewinding before it goes back: tape moves
 * from the right reel to the left as [progress] goes 0 to 1. Turn either reel with a finger
 * (anticlockwise, as rewinding does) or let [onTurn] be driven by a held button.
 */
@Composable
fun Reels(progress: Float, onTurn: (Float) -> Unit, modifier: Modifier = Modifier, spin: Float = 0f) {
    val c = U.c
    var last by remember { mutableFloatStateOf(Float.NaN) }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { last = Float.NaN },
                    onDragEnd = { last = Float.NaN },
                ) { change, _ ->
                    val p = change.position
                    // Around whichever reel is nearer.
                    val cx = if (p.x < size.width / 2) size.width * 0.3f else size.width * 0.7f
                    val cy = size.height * 0.47f
                    val a = atan2(p.y - cy, p.x - cx)
                    if (!last.isNaN()) {
                        var d = a - last
                        if (d > PI) d -= (2 * PI).toFloat()
                        if (d < -PI) d += (2 * PI).toFloat()
                        // Anticlockwise rewinds; a full turn is a twelfth of the tape.
                        if (d < 0) onTurn((-d / (2 * PI).toFloat()) / 12f)
                    }
                    last = a
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // The shell.
            drawRoundRect(Color(0xFF26222C), size = size, cornerRadius = CornerRadius(w * 0.04f))
            drawRoundRect(Color(0xFF3A3442), Offset(w * 0.03f, h * 0.05f), Size(w * 0.94f, h * 0.9f), CornerRadius(w * 0.03f), style = Stroke(2f))
            // The label, with lines for a title nobody wrote.
            drawRoundRect(c.paper, Offset(w * 0.08f, h * 0.08f), Size(w * 0.84f, h * 0.2f), CornerRadius(w * 0.01f))
            drawRect(c.neon, Offset(w * 0.08f, h * 0.24f), Size(w * 0.84f, h * 0.04f))
            repeat(2) { i -> drawLine(c.paperLine, Offset(w * 0.12f, h * (0.14f + i * 0.05f)), Offset(w * 0.88f, h * (0.14f + i * 0.05f)), 1.5f) }
            // The window.
            val win = Offset(w * 0.18f, h * 0.32f)
            val winSize = Size(w * 0.64f, h * 0.32f)
            drawRoundRect(Color(0xFF0E0C12), win, winSize, CornerRadius(h * 0.16f))
            // Tape on each reel: left grows, right shrinks.
            val hub = h * 0.07f
            val full = h * 0.25f
            val left = sqrt(hub * hub + (full * full - hub * hub) * progress)
            val right = sqrt(hub * hub + (full * full - hub * hub) * (1f - progress))
            val l = Offset(w * 0.3f, h * 0.47f)
            val r = Offset(w * 0.7f, h * 0.47f)
            drawCircle(Color(0xFF4A2E1E), left, l)
            drawCircle(Color(0xFF4A2E1E), right, r)
            drawCircle(Color(0xFF3B2416), left, l, style = Stroke(1.5f))
            drawCircle(Color(0xFF3B2416), right, r, style = Stroke(1.5f))
            hubs(l, hub, spin)
            hubs(r, hub, spin * (left / right.coerceAtLeast(1f)))
            // Screws in the corners.
            listOf(Offset(w * 0.06f, h * 0.9f), Offset(w * 0.94f, h * 0.9f), Offset(w * 0.5f, h * 0.88f)).forEach { drawCircle(Color(0xFF5A5363), h * 0.022f, it) }
            // The head opening at the bottom.
            drawPath(Path().apply { moveTo(w * 0.22f, h); lineTo(w * 0.28f, h * 0.76f); lineTo(w * 0.72f, h * 0.76f); lineTo(w * 0.78f, h); close() }, Color(0xFF1C1921))
        }
    }
}

private fun DrawScope.hubs(c: Offset, r: Float, angle: Float) {
    drawCircle(Color(0xFFE9E4DA), r, c)
    rotate(-angle, c) {
        repeat(6) { i ->
            val a = i * PI.toFloat() / 3f
            drawLine(Color(0xFF26222C), c + Offset(cos(a) * r * 0.45f, sin(a) * r * 0.45f), c + Offset(cos(a) * r, sin(a) * r), r * 0.22f)
        }
    }
    drawCircle(Color(0xFF26222C), r * 0.35f, c)
}

/**
 * The clerk's pick, wrapped in brown paper and tied with string, a question mark in marker on it.
 * [torn] 0..1 peels the paper back from the top as it's tapped open.
 */
@Composable
fun Parcel(hint: String, torn: Float, modifier: Modifier = Modifier) {
    val c = U.c
    Box(modifier.aspectRatio(0.62f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rnd = Random(7)
            drawRoundRect(c.kraft, size = size, cornerRadius = CornerRadius(6f))
            // Fibres and folds.
            repeat(40) {
                val a = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h)
                drawLine(Color(0xFF9C7246).copy(alpha = 0.35f), a, a + Offset((rnd.nextFloat() - 0.5f) * w * 0.3f, (rnd.nextFloat() - 0.5f) * h * 0.05f), 0.8f)
            }
            drawLine(Color(0xFF8E6639).copy(alpha = 0.5f), Offset(0f, h * 0.18f), Offset(w, h * 0.16f), 2f)
            drawLine(Color(0xFF8E6639).copy(alpha = 0.5f), Offset(0f, h * 0.84f), Offset(w, h * 0.86f), 2f)
            // String, crossed and knotted.
            val string = Color(0xFFEFE6D2)
            drawLine(string, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), 4f)
            drawLine(string, Offset(0f, h * 0.55f), Offset(w, h * 0.55f), 4f)
            drawCircle(string, 9f, Offset(w * 0.5f, h * 0.55f))
            drawLine(string, Offset(w * 0.5f, h * 0.55f), Offset(w * 0.38f, h * 0.63f), 4f)
            drawLine(string, Offset(w * 0.5f, h * 0.55f), Offset(w * 0.63f, h * 0.62f), 4f)
            // Torn open from the top: the box's dark plastic shows through.
            if (torn > 0f) {
                val depth = h * 0.85f * torn
                val path = Path().apply {
                    moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, depth * 0.8f)
                    val teeth = 9
                    for (i in teeth downTo 0) lineTo(w * i / teeth, depth * (0.8f + 0.2f * ((i % 2) * rnd.nextFloat())))
                    close()
                }
                drawPath(path, Color(0xFF141217))
            }
        }
        if (torn < 0.4f) {
            val ink = Color(0xFF2B1D10)
            Text("?", style = TextStyle(fontFamily = Hand, fontSize = 96.sp, color = ink), modifier = Modifier.rotate(-8f).alpha(1f - torn * 2f))
            Text(hint, style = UType.Note.copy(color = ink, fontSize = 22.sp, textAlign = TextAlign.Center), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 18.dp).alpha(1f - torn * 2f).rotate(-3f))
        }
    }
}

/** A round rubber stamp, a little crooked, for the library card and the receipt. */
@Composable
fun Stamp(text: String, color: Color, modifier: Modifier = Modifier, angle: Float = -8f, faint: Boolean = false) {
    Box(modifier.rotate(angle).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val a = if (faint) 0.45f else 0.85f
            drawCircle(color.copy(alpha = a), size.minDimension / 2 - 2f, style = Stroke(2.5f))
            drawCircle(color.copy(alpha = a), size.minDimension / 2 - 7f, style = Stroke(1f))
        }
        Text(text, style = TextStyle(color = color.copy(alpha = if (faint) 0.5f else 0.9f), fontFamily = Block, textAlign = TextAlign.Center, fontSize = 10.sp, lineHeight = 11.sp))
    }
}

/** The plank a row of boxes stands on, with the price rail along its front. */
@Composable
fun ShelfLedge(modifier: Modifier = Modifier) {
    val c = U.c
    Canvas(modifier.fillMaxWidth().aspectRatio(40f)) {
        drawRect(c.shelfEdge, size = Size(size.width, size.height * 0.55f))
        drawRect(c.shelf, Offset(0f, size.height * 0.55f), Size(size.width, size.height * 0.45f))
        drawLine(c.cyan.copy(alpha = 0.18f), Offset(0f, 1f), Offset(size.width, 1f), 2f)
    }
}

/** A dot-matrix till's line of dashes, between parts of a receipt. */
@Composable
fun Dashes(modifier: Modifier = Modifier, color: Color = Color(0xFF8A8394)) {
    Canvas(modifier.fillMaxWidth().aspectRatio(120f)) {
        var x = 0f
        val dash = 6.dp.toPx()
        while (x < size.width) {
            drawLine(color, Offset(x, size.height / 2), Offset((x + dash * 0.6f).coerceAtMost(size.width), size.height / 2), 1.5f)
            x += dash
        }
    }
}

/** The zigzag edge of torn-off till paper. */
fun DrawScope.zigzag(color: Color, top: Boolean) {
    val step = 10.dp.toPx()
    val depth = 5.dp.toPx()
    val path = Path()
    if (top) {
        path.moveTo(0f, depth)
        var x = 0f
        while (x < size.width) { path.lineTo(x + step / 2, 0f); path.lineTo(x + step, depth); x += step }
        path.lineTo(size.width, size.height); path.lineTo(0f, size.height)
    } else {
        path.moveTo(0f, 0f); path.lineTo(size.width, 0f); path.lineTo(size.width, size.height - depth)
        var x = size.width
        while (x > 0) { path.lineTo(x - step / 2, size.height); path.lineTo(x - step, size.height - depth); x -= step }
    }
    path.close()
    drawPath(path, color)
}
