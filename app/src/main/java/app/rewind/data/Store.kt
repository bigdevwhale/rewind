package app.rewind.data

import android.content.Context
import app.rewind.domain.Draft
import app.rewind.domain.Rules
import app.rewind.notify.Reminders
import kotlin.random.Random

/** Bringing tapes in, renting them out, taking them back, marking them down. */
class Store(private val context: Context, private val db: StoreDatabase, private val settings: SettingsStore) {
    private val dao get() = db.dao()

    /** A new tape: into the window for a few days, or straight onto the shelf. */
    suspend fun add(d: Draft, now: Long = System.currentTimeMillis()): Long {
        val days = settings.now().windowDays
        val cover = d.coverUrl?.let { Catalog.fetchCover(context, it) }
        val id = dao.insert(
            Tape(
                kind = d.kind.name,
                title = d.title.trim(),
                creator = d.creator.trim(),
                year = d.year,
                minutes = d.minutes,
                blurb = d.blurb.trim(),
                from = d.from.trim(),
                seed = Random.nextLong(),
                addedAt = now,
                readyAt = now + days * Rules.DAY,
                link = d.link,
                cover = cover,
            ),
        )
        Reminders.schedule(context)
        return id
    }

    suspend fun edit(id: Long, d: Draft) {
        val t = dao.tape(id) ?: return
        // A new cover picked from the catalogue replaces the old one.
        val cover = d.coverUrl?.let { Catalog.fetchCover(context, it) }?.also { Covers.delete(context, t.cover) } ?: t.cover
        dao.update(t.copy(cover = cover, link = d.link ?: t.link, kind = d.kind.name, title = d.title.trim(), creator = d.creator.trim(), year = d.year, minutes = d.minutes, blurb = d.blurb.trim(), from = d.from.trim()))
    }

    suspend fun rent(id: Long, mystery: Boolean = false, now: Long = System.currentTimeMillis()): Long {
        val t = dao.tape(id) ?: return 0
        val r = dao.insert(Rental(tapeId = id, outAt = now, dueAt = Rules.due(t.kindOf, now), mystery = mystery))
        if (t.placeOf == Place.BIN) dao.update(t.copy(place = Place.STORE.name, binAt = 0))
        Reminders.schedule(context)
        return r
    }

    /** Back over the counter, with its slip. */
    suspend fun giveBack(rentalId: Long, outcome: Outcome, words: String, scene: String, note: String) {
        val r = dao.rental(rentalId) ?: return
        dao.update(r.copy(backAt = System.currentTimeMillis(), outcome = outcome.name, words = words.trim(), scene = scene.trim(), note = note.trim()))
        Reminders.schedule(context)
    }

    /** Taken by mistake, a moment ago: back on the shelf as if it never left. */
    suspend fun undoRent(rentalId: Long) {
        dao.deleteRental(rentalId)
        Reminders.schedule(context)
    }

    suspend fun markDown(id: Long) {
        val t = dao.tape(id) ?: return
        dao.update(t.copy(place = Place.BIN.name, binAt = System.currentTimeMillis()))
    }

    /** Out of the bin and back on the shelf, fresh as new arrival. */
    suspend fun rescue(id: Long) {
        val t = dao.tape(id) ?: return
        val now = System.currentTimeMillis()
        dao.update(t.copy(place = Place.STORE.name, binAt = 0, readyAt = now))
    }

    suspend fun forget(id: Long) {
        val t = dao.tape(id) ?: return
        dao.forget(id)
        Covers.delete(context, t.cover)
    }
}
