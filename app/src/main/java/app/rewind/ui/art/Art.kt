package app.rewind.ui.art

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.rewind.BuildConfig
import app.rewind.data.Covers
import app.rewind.ui.ctx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

object Art {
    /** Screenshots and renders: the neon holds steady and nothing turns. */
    @Volatile var still = BuildConfig.SHOTS

    /**
     * A drawn box's colours, from its seed: the loud two-colour printing of a nineties tape, a
     * ground, a band and the title's ink.
     */
    data class Scheme(val ground: Color, val band: Color, val ink: Color, val accent: Color)

    private val schemes = listOf(
        Scheme(Color(0xFF14213D), Color(0xFFFCA311), Color(0xFFF5F0E4), Color(0xFFE63946)),
        Scheme(Color(0xFF2B0F3A), Color(0xFFFF4FA3), Color(0xFFFFF3F8), Color(0xFF52E6FF)),
        Scheme(Color(0xFF0B3D2E), Color(0xFFE9C46A), Color(0xFFF1FAEE), Color(0xFFE76F51)),
        Scheme(Color(0xFF7A1E1E), Color(0xFF111111), Color(0xFFFFE8C2), Color(0xFFFFB547)),
        Scheme(Color(0xFFEDE6D6), Color(0xFF1D3557), Color(0xFF1D1A20), Color(0xFFD62828)),
        Scheme(Color(0xFF0F1A2B), Color(0xFF52E6FF), Color(0xFFEAF6FF), Color(0xFFFF6B35)),
        Scheme(Color(0xFF3D2C1E), Color(0xFFF4A259), Color(0xFFFFF5E6), Color(0xFF5B8E7D)),
        Scheme(Color(0xFF111111), Color(0xFFE63946), Color(0xFFF5F5F5), Color(0xFFFFD166)),
        Scheme(Color(0xFF264653), Color(0xFF2A9D8F), Color(0xFFF7F3E3), Color(0xFFF4A261)),
        Scheme(Color(0xFF5E2A84), Color(0xFFFFD166), Color(0xFFFFF8E7), Color(0xFF06D6A0)),
    )

    fun scheme(seed: Long): Scheme = schemes[Random(seed).nextInt(schemes.size)]

    /** One of a few layouts for a drawn cover, from the seed. */
    fun layout(seed: Long): Int = Random(seed xor 0x5DEECE66DL).nextInt(4)
}

/** A tape's cover picture from the covers folder, decoded off the main thread. */
@Composable
fun rememberCover(name: String?): ImageBitmap? {
    val c = ctx()
    val bmp by produceState<ImageBitmap?>(null, name) {
        value = withContext(Dispatchers.IO) { Covers.file(c, name)?.let { decode(it.readBytes(), 600) } }
    }
    return bmp
}

/** A picture from the catalogue, for the search results and the new tape's preview; kept for the session. */
@Composable
fun rememberRemote(url: String?): ImageBitmap? {
    var bmp by remember(url) { mutableStateOf(url?.let { Remote.cache[it] }) }
    LaunchedEffect(url) {
        if (url == null || bmp != null) return@LaunchedEffect
        bmp = withContext(Dispatchers.IO) {
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.setRequestProperty("User-Agent", "Rewind/1.0 (https://github.com/bigdevwhale/rewind)")
                c.connectTimeout = 8_000
                c.readTimeout = 10_000
                val bytes = try { c.inputStream.readBytes() } finally { c.disconnect() }
                decode(bytes, 300)?.also { Remote.cache[url] = it }
            } catch (_: Exception) {
                null
            }
        }
    }
    return bmp
}

private object Remote {
    val cache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
}

private fun decode(bytes: ByteArray, max: Int): ImageBitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
    var sample = 1
    while (o.outWidth / (sample * 2) >= max && o.outHeight / (sample * 2) >= max) sample *= 2
    val b: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    return b.asImageBitmap()
}
