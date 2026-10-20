package app.rewind.data

import android.content.Context
import android.graphics.BitmapFactory
import app.rewind.domain.Draft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Looking a title up, so a tape comes in with its cover, year, maker and length. All free and
 * keyless: films and series from Wikidata (posters from Wikipedia), series also from TVmaze, albums
 * and books from Apple's iTunes Search, books also from Wikidata. This is the only thing that goes
 * online, and only when someone searches.
 */
object Catalog {
    private const val AGENT = "Rewind/1.0 (https://github.com/bigdevwhale/rewind)"

    private val films = setOf("Q11424", "Q202866", "Q24869", "Q506240", "Q93204", "Q17517379", "Q226730")
    private val series = setOf("Q5398426", "Q581714", "Q1259759", "Q526877", "Q117467246", "Q63952888")
    private val books = setOf("Q7725634", "Q8261", "Q571", "Q47461344", "Q49084", "Q1667921", "Q5185279", "Q25379", "Q12308638")

    suspend fun search(kind: Kind, query: String, ru: Boolean): List<Draft> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val found = coroutineScope {
            when (kind) {
                Kind.FILM -> listOf(async { wikidata(q, kind, films, ru) })
                Kind.SERIES -> listOf(async { tvmaze(q) }, async { wikidata(q, kind, series, ru) })
                Kind.ALBUM -> listOf(async { itunes(q, kind, ru) })
                Kind.BOOK -> listOf(async { wikidata(q, kind, books, ru) }, async { itunes(q, kind, ru) })
            }.awaitAll()
        }
        // Each source in turn, best first, without the same title twice.
        val seen = HashSet<String>()
        val merged = ArrayList<Draft>()
        val lists = found.map { it.toMutableList() }
        while (lists.any { it.isNotEmpty() }) for (l in lists) if (l.isNotEmpty()) {
            val d = l.removeAt(0)
            if (seen.add(d.title.lowercase() + "|" + (d.year ?: ""))) merged += d
        }
        merged.take(16)
    }

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", AGENT)
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        try {
            if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null
        } finally {
            c.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun year(date: String?): Int? = date?.let { Regex("""(\d{4})""").find(it)?.value?.toIntOrNull() }

    private suspend fun itunes(q: String, kind: Kind, ru: Boolean): List<Draft> {
        val what = if (kind == Kind.ALBUM) "entity=album" else "media=ebook"
        val country = if (ru) "&country=ru" else ""
        val body = get("https://itunes.apple.com/search?term=${enc(q)}&$what&limit=12$country") ?: return emptyList()
        val res = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return (0 until res.length()).map { res.getJSONObject(it) }.map { o ->
            val title = if (kind == Kind.ALBUM) o.optString("collectionName") else o.optString("trackName")
            Draft(
                kind = kind,
                title = title.replace(Regex("""\s*[-–]\s*(Single|EP)$"""), ""),
                creator = o.optString("artistName"),
                year = year(o.optString("releaseDate")),
                minutes = null,
                link = o.optString(if (kind == Kind.ALBUM) "collectionViewUrl" else "trackViewUrl").ifBlank { null },
                // The 100 px thumbnail comes in any size asked for.
                coverUrl = o.optString("artworkUrl100").ifBlank { null }?.replace("100x100bb", "600x600bb"),
            )
        }.filter { it.title.isNotBlank() && relevant(q, it) }
    }

    /** iTunes fills a short list with whatever sells; keep what shares a word with the query. */
    private fun relevant(q: String, d: Draft): Boolean {
        val words = q.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.length >= 3 }
        if (words.isEmpty()) return true
        val hay = (d.title + " " + d.creator).lowercase()
        return words.any { it in hay }
    }

    private suspend fun tvmaze(q: String): List<Draft> {
        val body = get("https://api.tvmaze.com/search/shows?q=${enc(q)}") ?: return emptyList()
        val arr = JSONArray(body)
        return (0 until arr.length()).map { arr.getJSONObject(it).getJSONObject("show") }.map { s ->
            val net = s.optJSONObject("network")?.optString("name") ?: s.optJSONObject("webChannel")?.optString("name") ?: ""
            Draft(
                kind = Kind.SERIES,
                title = s.optString("name"),
                creator = net,
                year = year(s.optString("premiered")),
                minutes = s.optInt("averageRuntime").takeIf { it > 0 },
                link = s.optString("url").ifBlank { null },
                coverUrl = s.optJSONObject("image")?.optString("medium")?.ifBlank { null },
            )
        }.take(8)
    }

    private suspend fun wikidata(q: String, kind: Kind, types: Set<String>, ru: Boolean): List<Draft> = coroutineScope {
        val lang = if (ru) "ru" else "en"
        val body = get("https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${enc(q)}&language=$lang&uselang=$lang&type=item&limit=20&format=json")
            ?: return@coroutineScope emptyList()
        val ids = JSONObject(body).optJSONArray("search")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
        if (ids.isEmpty()) return@coroutineScope emptyList()
        val ents = get("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${ids.joinToString("|")}&props=claims|labels|sitelinks&languages=ru|en&sitefilter=enwiki|ruwiki&format=json")
            ?.let { JSONObject(it).optJSONObject("entities") } ?: return@coroutineScope emptyList()
        val makerProp = when (kind) {
            Kind.FILM -> listOf("P57")
            Kind.SERIES -> listOf("P170", "P57", "P449")
            Kind.BOOK -> listOf("P50")
            Kind.ALBUM -> listOf("P175")
        }
        class Raw(val label: String, val year: Int?, val minutes: Int?, val maker: String?, val enwiki: String?, val ruwiki: String?, val image: String?)
        val raws = ids.mapNotNull { id ->
            val e = ents.optJSONObject(id) ?: return@mapNotNull null
            val claims = e.optJSONObject("claims") ?: return@mapNotNull null
            if (values(claims, "P31").none { it in types }) return@mapNotNull null
            val labels = e.optJSONObject("labels")
            val label = labels?.optJSONObject(lang)?.optString("value") ?: labels?.optJSONObject("en")?.optString("value") ?: labels?.optJSONObject("ru")?.optString("value") ?: return@mapNotNull null
            // Publication date, or for a series the date it started.
            val time = listOf("P577", "P580").firstNotNullOfOrNull { p -> claims.optJSONArray(p)?.let { first(it)?.optString("time") } }
            val dur = claims.optJSONArray("P2047")?.let { first(it) }?.let { v ->
                val n = v.optString("amount").toDoubleOrNull() ?: return@let null
                if (v.optString("unit").endsWith("Q11574")) (n / 60).toInt() else if (v.optString("unit").endsWith("Q25235")) (n * 60).toInt() else n.toInt()
            }?.takeIf { it in 1..2000 }
            val maker = makerProp.firstNotNullOfOrNull { p -> values(claims, p).firstOrNull() }
            val links = e.optJSONObject("sitelinks")
            val image = claims.optJSONArray("P18")?.let { a -> a.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optString("value") }
            Raw(label, year(time), dur, maker, links?.optJSONObject("enwiki")?.optString("title"), links?.optJSONObject("ruwiki")?.optString("title"), image)
        }.take(8)
        // The makers' names, in one more request.
        val makers = raws.mapNotNull { it.maker }.distinct()
        val names = if (makers.isEmpty()) emptyMap() else get("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${makers.joinToString("|")}&props=labels&languages=$lang|en&format=json")
            ?.let { JSONObject(it).optJSONObject("entities") }
            ?.let { es -> makers.associateWith { id -> es.optJSONObject(id)?.optJSONObject("labels")?.let { l -> l.optJSONObject(lang) ?: l.optJSONObject("en") }?.optString("value") ?: "" } }
            .orEmpty()
        raws.map { r ->
            async {
                Draft(
                    kind = kind,
                    title = r.label,
                    creator = r.maker?.let { names[it] } ?: "",
                    year = r.year,
                    minutes = r.minutes,
                    link = (r.ruwiki?.takeIf { ru } ?: r.enwiki)?.let { t -> "https://${if (ru && r.ruwiki != null) "ru" else "en"}.wikipedia.org/wiki/${enc(t.replace(' ', '_'))}" },
                    coverUrl = poster(r.enwiki, "en") ?: poster(r.ruwiki, "ru") ?: r.image?.let { "https://commons.wikimedia.org/wiki/Special:FilePath/${enc(it.replace(' ', '_'))}?width=400" },
                )
            }
        }.awaitAll()
    }

    /** The picture at the top of the Wikipedia article: for a film, usually its poster. */
    private fun poster(title: String?, lang: String): String? {
        title ?: return null
        val body = get("https://$lang.wikipedia.org/api/rest_v1/page/summary/${enc(title.replace(' ', '_')).replace("+", "%20")}") ?: return null
        return JSONObject(body).optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
    }

    private fun first(claims: JSONArray): JSONObject? = claims.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")

    private fun values(claims: JSONObject, prop: String): List<String> {
        val a = claims.optJSONArray(prop) ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")?.optString("id") }
    }

    /** Downloads a cover into the covers folder; the file's name, or null. */
    suspend fun fetchCover(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", AGENT)
            c.connectTimeout = 8_000
            c.readTimeout = 15_000
            val bytes = try {
                if (c.responseCode !in 200..299) return@withContext null
                c.inputStream.readBytes()
            } finally {
                c.disconnect()
            }
            if (BitmapFactory.decodeByteArray(bytes, 0, bytes.size) == null) return@withContext null
            val name = UUID.randomUUID().toString() + ".jpg"
            File(Covers.dir(context), name).writeBytes(bytes)
            name
        } catch (_: Exception) {
            null
        }
    }
}

/** Cover pictures, kept in the app's own folder. */
object Covers {
    fun dir(context: Context): File = File(context.filesDir, "covers").apply { mkdirs() }

    fun file(context: Context, name: String?): File? = name?.let { File(dir(context), it) }?.takeIf { it.exists() }

    fun delete(context: Context, name: String?) {
        file(context, name)?.delete()
    }
}
