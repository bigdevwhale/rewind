package app.rewind

import app.rewind.data.Kind
import app.rewind.data.Tape
import app.rewind.domain.Hours
import app.rewind.domain.Mystery
import app.rewind.domain.Rules
import app.rewind.domain.Shares
import app.rewind.domain.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.random.Random

class HoursTest {
    // Tuesday 20 October 2026; Saturday 24 October 2026.
    private val h = Hours(on = true, weekday = 18, weekend = 12, close = 23)

    @Test fun eveningsOnWeekdays() {
        assertFalse(h.open(LocalDateTime.of(2026, 10, 20, 17, 59)))
        assertTrue(h.open(LocalDateTime.of(2026, 10, 20, 18, 0)))
        assertTrue(h.open(LocalDateTime.of(2026, 10, 20, 22, 59)))
        assertFalse(h.open(LocalDateTime.of(2026, 10, 20, 23, 0)))
    }

    @Test fun middayAtTheWeekend() {
        assertTrue(h.open(LocalDateTime.of(2026, 10, 24, 12, 30)))
        assertFalse(h.open(LocalDateTime.of(2026, 10, 23, 12, 30)))
    }

    @Test fun closingPastMidnight() {
        val late = h.copy(close = 1)
        assertTrue(late.open(LocalDateTime.of(2026, 10, 21, 0, 30)))
        assertFalse(late.open(LocalDateTime.of(2026, 10, 21, 1, 30)))
        assertEquals(LocalDateTime.of(2026, 10, 21, 1, 0), late.closing(LocalDateTime.of(2026, 10, 20, 23, 30)))
    }

    @Test fun nextOpening() {
        assertEquals(LocalDateTime.of(2026, 10, 20, 18, 0), h.nextOpening(LocalDateTime.of(2026, 10, 20, 9, 0)))
        // Friday night after closing: Saturday at noon.
        assertEquals(LocalDateTime.of(2026, 10, 24, 12, 0), h.nextOpening(LocalDateTime.of(2026, 10, 23, 23, 30)))
    }

    @Test fun offNeverCloses() {
        assertTrue(h.copy(on = false).open(LocalDateTime.of(2026, 10, 20, 4, 0)))
    }
}

class RulesTest {
    @Test fun loans() {
        assertEquals(48 * Rules.HOUR, Rules.due(Kind.FILM, 0))
        assertEquals(7 * Rules.DAY, Rules.due(Kind.SERIES, 0))
        assertEquals(14 * Rules.DAY, Rules.due(Kind.BOOK, 0))
    }

    @Test fun fadesAfterAMonthAndIsBleachedByHalfAYear() {
        assertEquals(0f, Rules.fade(0, 20 * Rules.DAY))
        assertEquals(0.5f, Rules.fade(0, 105 * Rules.DAY), 0.001f)
        assertEquals(1f, Rules.fade(0, 200 * Rules.DAY))
    }

    @Test fun theClerkPicksWhatFits() {
        val long = Tape(id = 1, title = "Long", kind = Kind.FILM.name, minutes = 200)
        val short = Tape(id = 2, title = "Short", kind = Kind.FILM.name, minutes = 50)
        val album = Tape(id = 3, title = "Album", kind = Kind.ALBUM.name)
        val shelf = listOf(long, short, album)
        repeat(20) { i ->
            val t = Mystery.pick(shelf, Time.SHORT, null, { 0f }, Random(i))
            assertTrue(t == short || t == album)
        }
        assertEquals(album, Mystery.pick(shelf, Time.LONG, Kind.ALBUM, { 0f }))
        assertNull(Mystery.pick(shelf, Time.SHORT, Kind.BOOK, { 0f }))
    }
}

class SharesTest {
    @Test fun aCardGoesThereAndBack() {
        val text = Shares.card(Kind.FILM, "Stalker", "Andrei Tarkovsky", 1979, 161, "Watch it in winter.", "Anya")
        val d = Shares.parse("Look:\n$text")
        assertEquals(Kind.FILM, d.kind)
        assertEquals("Stalker", d.title)
        assertEquals("Andrei Tarkovsky", d.creator)
        assertEquals(1979, d.year)
        assertEquals(161, d.minutes)
        assertEquals("Watch it in winter.", d.blurb)
        assertEquals("Anya", d.from)
    }

    @Test fun linksFromKnownSites() {
        val k = Shares.parse("Сталкер (1979) — Кинопоиск https://www.kinopoisk.ru/film/43911/")
        assertEquals(Kind.FILM, k.kind)
        assertEquals("Сталкер", k.title)
        assertEquals(1979, k.year)
        assertEquals(Kind.SERIES, Shares.parse("https://www.kinopoisk.ru/series/404900/").kind)
        val s = Shares.parse("https://open.spotify.com/album/1weenld61qoidwYuZ1GESA", "Kind of Blue - Album by Miles Davis | Spotify")
        assertEquals(Kind.ALBUM, s.kind)
        assertEquals("Kind of Blue", s.title)
        assertEquals("Miles Davis", s.creator)
        assertEquals(Kind.BOOK, Shares.parse("Dune https://www.goodreads.com/book/show/44767458-dune").kind)
        assertEquals(Kind.COMIC, Shares.parse("Berserk https://myanimelist.net/manga/2/Berserk").kind)
    }
}
