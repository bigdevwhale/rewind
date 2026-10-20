package app.rewind.domain

import app.rewind.data.Kind

/** A tape about to be brought in: from a share, a friend's card, or typed. */
data class Draft(
    val kind: Kind = Kind.FILM,
    val title: String = "",
    val creator: String = "",
    val year: Int? = null,
    val minutes: Int? = null,
    val blurb: String = "",
    val from: String = "",
    val link: String? = null,
    /** A cover picture from the catalogue, fetched when the tape is brought in. */
    val coverUrl: String? = null,
)

/**
 * Reading what's shared into the app, and writing the card a tape is recommended with.
 *
 * A card is plain text, so it travels through any messenger; whoever has the app shares it back in
 * (Share → Rewind) and the tape lands on that friend's staff picks shelf:
 *
 *     📼 «Stalker» (1979) — Andrei Tarkovsky
 *     “Watch it in winter, phone in another room.”
 *     — Marat
 *     #rewind film 161
 */
object Shares {
    private const val TAG = "#rewind"
    private val url = Regex("""https?://\S+""")
    private val yearRe = Regex("""\((\d{4})\)""")

    fun card(kind: Kind, title: String, creator: String, year: Int?, minutes: Int?, blurb: String, name: String): String = buildString {
        append("📼 «").append(title.trim()).append('»')
        if (year != null) append(" (").append(year).append(')')
        if (creator.isNotBlank()) append(" — ").append(creator.trim())
        if (blurb.isNotBlank()) append("\n“").append(blurb.trim()).append('”')
        if (name.isNotBlank()) append("\n— ").append(name.trim())
        append('\n').append(TAG).append(' ').append(kind.name.lowercase())
        if (minutes != null) append(' ').append(minutes)
    }

    fun parse(text: String, subject: String? = null): Draft {
        if (text.contains(TAG)) return card(text)
        val link = url.find(text)?.value?.trimEnd('.', ',', ')')
        val kind = link?.let { kindOf(it) } ?: Kind.FILM
        val raw = listOfNotNull(subject, text.replace(url, " "))
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() } ?: ""
        var title = clean(raw.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "")
        var creator = ""
        // "Kind of Blue - Album by Miles Davis", "Dune by Frank Herbert".
        Regex("""^(.+?)\s*[-–—·|]?\s*(?:album|single|ep|book)?\s+by\s+(.+)$""", RegexOption.IGNORE_CASE).find(title)?.let {
            title = it.groupValues[1].trim().trimEnd('-', '–', '—', '·', '|').trim()
            creator = it.groupValues[2].trim()
        }
        val year = yearRe.find(title)?.groupValues?.get(1)?.toIntOrNull()
        title = title.replace(yearRe, "").trim().trim('«', '»', '"', '“', '”').trim()
        return Draft(kind = kind, title = title, creator = creator, year = year, link = link)
    }

    private fun card(text: String): Draft {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val head = lines.firstOrNull { it.contains('«') || it.startsWith("📼") } ?: lines.firstOrNull() ?: ""
        val title = Regex("""«([^»]+)»""").find(head)?.groupValues?.get(1)
            ?: head.removePrefix("📼").substringBefore(" — ").replace(yearRe, "").trim()
        val year = yearRe.find(head)?.groupValues?.get(1)?.toIntOrNull()
        val creator = head.substringAfter(" — ", "").trim()
        val blurb = lines.firstOrNull { it.startsWith("“") || it.startsWith("\"") || it.startsWith("„") }
            ?.trim('“', '”', '"', '„')?.trim() ?: ""
        val from = lines.firstOrNull { it.startsWith("— ") || it.startsWith("- ") }?.drop(2)?.trim() ?: ""
        val tag = lines.firstOrNull { it.startsWith(TAG) }?.removePrefix(TAG)?.trim()?.split(' ') ?: emptyList()
        val kind = tag.firstOrNull()?.let { k -> Kind.entries.firstOrNull { it.name.equals(k, true) } } ?: Kind.FILM
        val minutes = tag.getOrNull(1)?.toIntOrNull()
        return Draft(kind = kind, title = title, creator = creator, year = year, minutes = minutes, blurb = blurb, from = from)
    }

    /** What a link points at, by where it lives. */
    fun kindOf(link: String): Kind? {
        val l = link.lowercase()
        return when {
            listOf("comicvine", "myanimelist.net/manga", "shikimori.one/mangas", "mangalib", "remanga", "comixology", "webtoons", "marvel.com/comics").any { it in l } -> Kind.COMIC
            listOf("/series/", "myshows.me", "tvmaze").any { it in l } -> Kind.SERIES
            listOf("kinopoisk", "imdb.com", "letterboxd", "ivi.ru", "okko.tv", "themoviedb").any { it in l } -> Kind.FILM
            listOf("spotify", "music.yandex", "music.apple", "bandcamp", "deezer", "music.youtube", "soundcloud", "tidal").any { it in l } -> Kind.ALBUM
            listOf("goodreads", "litres", "livelib", "bookmate", "openlibrary", "storytel").any { it in l } -> Kind.BOOK
            else -> null
        }
    }

    private val sites = listOf(
        "Кинопоиск", "КиноПоиск", "IMDb", "Letterboxd", "Spotify", "Яндекс Музыка", "Яндекс Музыке", "Yandex Music",
        "Apple Music", "Goodreads", "LiveLib", "Литрес", "ЛитРес", "Bookmate", "YouTube Music", "Bandcamp",
    )

    /** Drops the site's name from a page title: "Stalker (1979) — Кинопоиск", "Listen to … on Spotify". */
    private fun clean(s: String): String {
        var t = s
        for (site in sites) t = t.replace(Regex("""\s*(?:[-–—|·:]\s*)?(?:on\s+|в\s+|на\s+)?${Regex.escape(site)}\s*$""", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("""^(?:слушайте|слушай|listen to|watch|смотрите)\s+(?:альбом\s+|album\s+)?""", RegexOption.IGNORE_CASE), "")
        return t.trim().trim('"', '«', '»')
    }
}
