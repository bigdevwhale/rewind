package app.rewind.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.rewind.R
import app.rewind.data.Kind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Words and dates in the app's language. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]
    private fun zone() = ZoneId.systemDefault()

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "24 October 2026", or without the year for this year. */
    fun date(c: Context, d: LocalDate, today: LocalDate = LocalDate.now()): String =
        d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", locale(c)))

    fun date(c: Context, at: Long): String = date(c, Instant.ofEpochMilli(at).atZone(zone()).toLocalDate())

    /** "17.10", for a stamp. */
    fun short(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM"))

    /** "21:00". */
    fun time(c: Context, at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", locale(c)))

    fun time(c: Context, t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("HH:mm", locale(c)))

    /** "Fri 21:00", "today 21:00", "tomorrow 21:00". */
    fun when_(c: Context, at: Long): String {
        val t = Instant.ofEpochMilli(at).atZone(zone()).toLocalDateTime()
        val today = LocalDate.now(zone())
        val day = when (t.toLocalDate()) {
            today -> c.getString(R.string.today)
            today.plusDays(1) -> c.getString(R.string.tomorrow)
            else -> t.format(DateTimeFormatter.ofPattern("EEE d MMM", locale(c))).trimEnd('.')
        }
        return day + " " + time(c, t)
    }

    /** "1 h 50 min" or "45 min". */
    fun minutes(c: Context, min: Int): String =
        if (min < 60) c.getString(R.string.minutes_short, min)
        else if (min % 60 == 0) c.getString(R.string.hours_only, min / 60)
        else c.getString(R.string.hours_short, min / 60, min % 60)

    /** How long until [at]: "2 days", "5 h", "40 min". */
    fun left(c: Context, ms: Long): String {
        val min = (ms / 60_000).toInt().coerceAtLeast(0)
        return when {
            min >= 48 * 60 -> plural(c, R.plurals.days, min / (24 * 60))
            min >= 60 -> c.getString(R.string.hours_left, min / 60)
            else -> c.getString(R.string.minutes_short, min)
        }
    }

    fun kind(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.FILM -> R.string.kind_film
            Kind.SERIES -> R.string.kind_series
            Kind.ALBUM -> R.string.kind_album
            Kind.BOOK -> R.string.kind_book
        },
    )

    /** The aisle's name: "Films", "Series", "Music", "Books". */
    fun aisle(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.FILM -> R.string.aisle_film
            Kind.SERIES -> R.string.aisle_series
            Kind.ALBUM -> R.string.aisle_album
            Kind.BOOK -> R.string.aisle_book
        },
    )

    /** "2 evenings", "a week", "2 weeks". */
    fun loan(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.FILM, Kind.ALBUM -> R.string.loan_two_days
            Kind.SERIES -> R.string.loan_week
            Kind.BOOK -> R.string.loan_two_weeks
        },
    )
}

object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
