package app.rewind.domain

import app.rewind.data.Kind
import app.rewind.data.Rental
import app.rewind.data.Tape
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * The store's hours: evenings on weekdays, from midday at the weekend, closing at [close] (past
 * midnight when it's at or before the opening hour). With [on] off it never closes.
 */
data class Hours(val on: Boolean = true, val weekday: Int = 18, val weekend: Int = 12, val close: Int = 23) {
    private fun opens(d: LocalDate): Int = if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) weekend else weekday

    /** The evening's opening and closing that day. */
    fun session(d: LocalDate): Pair<LocalDateTime, LocalDateTime> {
        val open = opens(d)
        val start = d.atTime(open, 0)
        val end = if (close > open) d.atTime(close, 0) else d.plusDays(1).atTime(close, 0)
        return start to end
    }

    fun open(t: LocalDateTime): Boolean = !on || closing(t) != null

    /** When the store next opens (now, if it's open). */
    fun nextOpening(t: LocalDateTime): LocalDateTime {
        if (open(t)) return t
        var d = t.toLocalDate()
        repeat(9) {
            val start = session(d).first
            if (start.isAfter(t)) return start
            d = d.plusDays(1)
        }
        return t
    }

    /** When tonight's session ends, if open. */
    fun closing(t: LocalDateTime): LocalDateTime? {
        if (!on) return null
        val d = t.toLocalDate()
        return listOf(d.minusDays(1), d).map { session(it) }.firstOrNull { (a, b) -> !t.isBefore(a) && t.isBefore(b) }?.second
    }
}

object Rules {
    /** Tapes at home at once. */
    const val MAX_OUT = 2

    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR

    /** A tape untouched this long starts to fade in the window's sun... */
    const val FADE_FROM_DAYS = 30

    /** ...and this long is bleached through: time to mark it down. */
    const val FADE_FULL_DAYS = 180

    fun due(kind: Kind, outAt: Long): Long = outAt + kind.loanHours * HOUR

    /** When the tape was last in someone's hands: back from a rent, or brought in. Out now counts as now. */
    fun touched(t: Tape, rentals: List<Rental>, now: Long): Long =
        maxOf(t.readyAt, rentals.maxOfOrNull { if (it.out) now else it.backAt } ?: 0L)

    /** 0 fresh to 1 bleached, from the time on the shelf untouched. */
    fun fade(touched: Long, now: Long): Float {
        if (touched >= now) return 0f
        val days = (now - touched).toFloat() / DAY
        return ((days - FADE_FROM_DAYS) / (FADE_FULL_DAYS - FADE_FROM_DAYS)).coerceIn(0f, 1f)
    }

    /** 0 new to 1 worn soft, from the times it was seen through. */
    fun wear(watched: Int): Float = (watched / 6f).coerceIn(0f, 1f)
}

/** How much time there is tonight, for the tape in brown paper. */
enum class Time {
    /** An hour or so: an album, an episode, a chapter, a comic, a short film. */
    SHORT,

    /** An evening: anything up to two and a half hours. */
    EVENING,

    /** All the time in the world. */
    LONG;

    fun fits(t: Tape): Boolean = when (this) {
        SHORT -> t.kindOf in setOf(Kind.SERIES, Kind.BOOK, Kind.COMIC) || t.length <= 60
        EVENING -> t.kindOf in setOf(Kind.SERIES, Kind.BOOK, Kind.COMIC) || t.length <= 150
        LONG -> true
    }
}

object Mystery {
    /**
     * The clerk's pick from [shelf]: something that fits [time] (and [kind], if one was asked for),
     * leaning toward what has waited longest, so the faded ones get their evening too.
     */
    fun pick(shelf: List<Tape>, time: Time, kind: Kind?, fade: (Tape) -> Float, rnd: Random = Random.Default): Tape? {
        val fits = shelf.filter { time.fits(it) && (kind == null || it.kindOf == kind) }
        if (fits.isEmpty()) return null
        val weights = fits.map { 1f + 3f * fade(it) }
        var x = rnd.nextFloat() * weights.sum()
        fits.forEachIndexed { i, t ->
            x -= weights[i]
            if (x <= 0f) return t
        }
        return fits.last()
    }
}
