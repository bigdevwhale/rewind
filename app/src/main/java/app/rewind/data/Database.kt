package app.rewind.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * What a tape is, and so how it looks on the shelf and how long it can stay out: a film on a VHS
 * for two evenings, a series box set for a week, an album on an audio cassette for two evenings, a
 * book from the library corner for two weeks.
 */
enum class Kind(val loanHours: Int, val minutes: Int) {
    FILM(48, 110),
    SERIES(7 * 24, 45),
    ALBUM(48, 45),
    BOOK(14 * 24, 300);

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name == s } ?: FILM
    }
}

/** Where a tape is. Out on rent is a [Rental] with no return yet. */
enum class Place {
    /** In the store: in the window while it's new, then on the shelf. */
    STORE,

    /** Faded in the sun and marked down: the bargain bin, from where it can still be rescued. */
    BIN;

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name == s } ?: STORE
    }
}

/**
 * One thing to watch, hear or read. [blurb] is the back of the box, in the owner's words: why it's
 * worth taking. [from] is who recommended it ("" for the owner), and their picks make a shelf of
 * their own. Until [readyAt] it waits in the window ("coming soon").
 */
@Entity(tableName = "tapes", indices = [Index("place")])
data class Tape(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String = Kind.FILM.name,
    val title: String,
    val creator: String = "",
    val year: Int? = null,
    /** Running time (or reading time for a book), when known. */
    val minutes: Int? = null,
    val blurb: String = "",
    val from: String = "",
    val seed: Long = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val readyAt: Long = addedAt,
    val place: String = Place.STORE.name,
    val binAt: Long = 0,
    /** The link it was shared with, if any: to open it in the app it lives in. */
    val link: String? = null,
    /** A picture file in the app's covers folder, from the catalogue; without one the box is drawn from [seed]. */
    val cover: String? = null,
) {
    val kindOf: Kind get() = Kind.of(kind)
    val placeOf: Place get() = Place.of(place)
    val length: Int get() = minutes ?: kindOf.minutes
}

/** How a tape came back. */
enum class Outcome {
    /** Watched (heard, read) to the end. */
    WATCHED,

    /** Rewound and brought back unfinished, with a word on why. No shame in it. */
    REWOUND;

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name == s } ?: WATCHED
    }
}

/**
 * One rent: taken at [outAt], due at [dueAt], back at [backAt] (0 while out). The return slip is
 * [words] (three of them), [scene] (the one moment that stayed) and, for a rewound tape, [note].
 * [mystery]: it came wrapped in brown paper, picked by the clerk.
 */
@Entity(tableName = "rentals", indices = [Index("tapeId")])
data class Rental(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tapeId: Long,
    val outAt: Long = System.currentTimeMillis(),
    val dueAt: Long,
    val backAt: Long = 0,
    val outcome: String = "",
    val words: String = "",
    val scene: String = "",
    val note: String = "",
    val mystery: Boolean = false,
) {
    val out: Boolean get() = backAt == 0L
    val outcomeOf: Outcome? get() = if (out) null else Outcome.of(outcome)
    val late: Boolean get() = !out && backAt > dueAt
    fun overdue(now: Long): Boolean = out && now > dueAt
}

@Dao
interface StoreDao {
    @Query("SELECT * FROM tapes ORDER BY addedAt")
    fun observeTapes(): Flow<List<Tape>>

    @Query("SELECT * FROM tapes ORDER BY addedAt")
    suspend fun tapes(): List<Tape>

    @Query("SELECT * FROM tapes WHERE id = :id")
    suspend fun tape(id: Long): Tape?

    @Insert
    suspend fun insert(t: Tape): Long

    @Update
    suspend fun update(t: Tape)

    @Query("SELECT * FROM rentals ORDER BY outAt")
    fun observeRentals(): Flow<List<Rental>>

    @Query("SELECT * FROM rentals ORDER BY outAt")
    suspend fun rentals(): List<Rental>

    @Query("SELECT * FROM rentals WHERE id = :id")
    suspend fun rental(id: Long): Rental?

    @Insert
    suspend fun insert(r: Rental): Long

    @Insert
    suspend fun insertRentals(r: List<Rental>)

    @Update
    suspend fun update(r: Rental)

    @Query("DELETE FROM rentals WHERE id = :id")
    suspend fun deleteRental(id: Long)

    @Query("DELETE FROM rentals WHERE tapeId = :id")
    suspend fun deleteRentals(id: Long)

    @Query("DELETE FROM tapes WHERE id = :id")
    suspend fun deleteTape(id: Long)

    /** Thrown out of the bin for good, with its slips. */
    @Transaction
    suspend fun forget(id: Long) {
        deleteRentals(id)
        deleteTape(id)
    }
}

@Database(entities = [Tape::class, Rental::class], version = 1, exportSchema = true)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun dao(): StoreDao

    companion object {
        fun create(context: Context): StoreDatabase =
            Room.databaseBuilder(context.applicationContext, StoreDatabase::class.java, "rewind.db").build()
    }
}
