package app.rewind.data

import android.content.Context
import app.rewind.AppContainer
import app.rewind.domain.Rules
import app.rewind.ru
import java.time.LocalDate
import java.time.ZoneId

/**
 * A lived-in store for the demo build and the screenshots, as of an evening (8:30 pm): well-known
 * films, series, albums and books on the shelves with drawn boxes, a few months of slips, two tapes
 * at home, two in the window, a friend's shelf, one bleached by the sun and one in the bin.
 */
object DemoSeed {
    private class Item(
        val kind: Kind, val en: String, val ru: String, val creatorEn: String, val creatorRu: String, val year: Int, val minutes: Int?,
        val blurbEn: String = "", val blurbRu: String = "", val fromEn: String = "", val fromRu: String = "",
        /** Days since it was brought in. */
        val age: Int,
    )

    private val items = listOf(
        Item(Kind.FILM, "Stalker", "Сталкер", "Andrei Tarkovsky", "Андрей Тарковский", 1979, 161, "Watch it in winter, with the phone in another room.", "Смотреть зимой, телефон в другой комнате.", age = 150),
        Item(Kind.FILM, "Paris, Texas", "Париж, Техас", "Wim Wenders", "Вим Вендерс", 1984, 145, "The scene with the one-way mirror. Everyone says so.", "Сцена с зеркалом. Все про неё говорят.", age = 120),
        Item(Kind.FILM, "In the Mood for Love", "Любовное настроение", "Wong Kar-wai", "Вонг Карвай", 2000, 98, "For a rainy evening. The music alone.", "На дождливый вечер. Одна музыка чего стоит.", age = 96),
        Item(Kind.FILM, "Brother", "Брат", "Aleksei Balabanov", "Алексей Балабанов", 1997, 100, "Saw it at fifteen. Want to see it again at thirty.", "Видел в пятнадцать. Хочу посмотреть в тридцать.", age = 60),
        Item(Kind.FILM, "Spirited Away", "Унесённые призраками", "Hayao Miyazaki", "Хаяо Миядзаки", 2001, 125, age = 40),
        Item(Kind.FILM, "Groundhog Day", "День сурка", "Harold Ramis", "Гарольд Рамис", 1993, 101, "Every February.", "Каждый февраль.", age = 200),
        Item(Kind.SERIES, "Twin Peaks", "Твин Пикс", "ABC", "ABC", 1990, 47, "Damn fine coffee. One episode a night, no more.", "Чертовски хороший кофе. Одна серия за вечер, не больше.", age = 110),
        Item(Kind.SERIES, "Severance", "Разделение", "Apple TV", "Apple TV", 2022, 50, "Everyone at work talks about it.", "На работе все обсуждают.", age = 30),
        Item(Kind.ALBUM, "Kind of Blue", "Kind of Blue", "Miles Davis", "Майлз Дэвис", 1959, 46, "Sunday morning, with coffee.", "Воскресное утро, с кофе.", age = 140),
        Item(Kind.ALBUM, "Blood Type", "Группа крови", "Kino", "Кино", 1988, 42, "Side A in the car, loud.", "Первая сторона — в машине, громко.", age = 80),
        Item(Kind.ALBUM, "OK Computer", "OK Computer", "Radiohead", "Radiohead", 1997, 53, age = 50),
        Item(Kind.BOOK, "The Master and Margarita", "Мастер и Маргарита", "Mikhail Bulgakov", "Михаил Булгаков", 1967, 900, "Read it at school and understood nothing.", "Читал в школе и ничего не понял.", age = 130),
        Item(Kind.BOOK, "Roadside Picnic", "Пикник на обочине", "Arkady & Boris Strugatsky", "Аркадий и Борис Стругацкие", 1972, 300, "After Stalker, to see what it was really about.", "После «Сталкера» — понять, о чём это было на самом деле.", age = 90),
        Item(Kind.BOOK, "Stoner", "Стоунер", "John Williams", "Джон Уильямс", 1965, 420, age = 20),
        // A friend's picks.
        Item(Kind.FILM, "Perfect Days", "Идеальные дни", "Wim Wenders", "Вим Вендерс", 2023, 124, "You'll want to wash the dishes slowly afterwards.", "После него захочется медленно мыть посуду.", "Anya", "Аня", age = 12),
        Item(Kind.ALBUM, "Blue", "Blue", "Joni Mitchell", "Джони Митчелл", 1971, 36, "Headphones, the whole thing, lying down.", "В наушниках, целиком, лёжа.", "Anya", "Аня", age = 25),
        // In the window.
        Item(Kind.FILM, "Past Lives", "Прошлые жизни", "Celine Song", "Селин Сонг", 2023, 106, "The trailer made me cry. Brave.", "Плакал на трейлере. Смело.", age = 1),
        Item(Kind.SERIES, "The Bear", "Медведь", "FX", "FX", 2022, 30, age = 2),
        // Bleached: on the shelf seven months, never taken.
        Item(Kind.FILM, "Tenet", "Довод", "Christopher Nolan", "Кристофер Нолан", 2020, 150, "Everyone said I had to.", "Все говорили, что надо.", age = 215),
        // In the bin.
        Item(Kind.SERIES, "Game of Thrones", "Игра престолов", "HBO", "HBO", 2011, 57, age = 240),
    )

    private val slipsEn = listOf(
        "quiet · cold · water" to "The dog in the flooded room.",
        "lonely · red · forgiving" to "The phone call through the glass.",
        "rain · noodles · stairs" to "Her walking down the stairs in slow motion.",
        "Sunday · smoke · blue" to "So What, the first bass notes.",
        "loud · young · summer" to "Singing along in the car with the windows down.",
        "coffee · owls · fog" to "Cooper talking to his tape recorder.",
    )
    private val slipsRu = listOf(
        "тихо · холодно · вода" to "Собака в затопленной комнате.",
        "одиночество · красный · прощение" to "Телефонный разговор через стекло.",
        "дождь · лапша · лестница" to "Как она спускается по лестнице, замедленно.",
        "воскресенье · дым · синий" to "So What, первые ноты баса.",
        "громко · молодость · лето" to "Пели в машине с открытыми окнами.",
        "кофе · совы · туман" to "Купер говорит в диктофон.",
    )

    /** The evening the demo stands at: 8:30 pm today, or yesterday if that's still ahead. */
    fun base(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = LocalDate.now(zone).atTime(20, 30).atZone(zone).toInstant().toEpochMilli()
        return if (now >= today) today else today - Rules.DAY
    }

    suspend fun seedIfEmpty(c: Context, container: AppContainer) {
        val dao = container.db.dao()
        if (dao.tapes().isNotEmpty()) return
        val ru = c.ru
        val base = base()
        fun ago(days: Double) = base - (days * Rules.DAY).toLong()
        val ids = HashMap<String, Long>()
        items.forEachIndexed { i, it ->
            val added = ago(it.age.toDouble())
            ids[it.en] = dao.insert(
                Tape(
                    kind = it.kind.name, title = if (ru) it.ru else it.en, creator = if (ru) it.creatorRu else it.creatorEn, year = it.year, minutes = it.minutes,
                    blurb = if (ru) it.blurbRu else it.blurbEn, from = if (ru) it.fromRu else it.fromEn, seed = 1990L + i * 7919L,
                    addedAt = added, readyAt = added + 3 * Rules.DAY,
                    place = if (it.en == "Game of Thrones") Place.BIN.name else Place.STORE.name, binAt = if (it.en == "Game of Thrones") ago(14.0) else 0,
                ),
            )
        }
        val slips = if (ru) slipsRu else slipsEn
        suspend fun back(title: String, outDaysAgo: Double, hours: Double, slip: Int?, outcome: Outcome = Outcome.WATCHED, note: String = "", mystery: Boolean = false) {
            val id = ids.getValue(title)
            val kind = items.first { it.en == title }.kind
            val out = ago(outDaysAgo)
            val (w, s) = slip?.let { slips[it] } ?: ("" to "")
            dao.insert(Rental(tapeId = id, outAt = out, dueAt = Rules.due(kind, out), backAt = out + (hours * Rules.HOUR).toLong(), outcome = outcome.name, words = w, scene = s, note = note, mystery = mystery))
        }
        back("Stalker", 140.0, 30.0, 0)
        back("Groundhog Day", 120.0, 26.0, null)
        back("Paris, Texas", 100.0, 52.0, 1)
        back("Kind of Blue", 95.0, 20.0, 3)
        back("Twin Peaks", 88.0, 160.0, 5)
        back("Kind of Blue", 70.0, 40.0, null)
        back("In the Mood for Love", 62.0, 22.0, 2, mystery = true)
        back("Blood Type", 55.0, 30.0, 4)
        back("The Master and Margarita", 50.0, 300.0, null, Outcome.REWOUND, if (ru) "Не то время года. Вернусь осенью." else "Wrong season for it. Back in the autumn.")
        back("Stalker", 30.0, 44.0, null)
        back("Kind of Blue", 20.0, 18.0, null)
        back("Roadside Picnic", 18.0, 200.0, null)
        back("Blue", 9.0, 26.0, null)
        back("Kind of Blue", 6.0, 22.0, null)
        back("Spirited Away", 4.0, 30.0, null, mystery = true)
        // At home now: a film due tomorrow evening, and a series with five days to go.
        dao.insert(Rental(tapeId = ids.getValue("Perfect Days"), outAt = ago(1.0), dueAt = Rules.due(Kind.FILM, ago(1.0))))
        dao.insert(Rental(tapeId = ids.getValue("Severance"), outAt = ago(2.0), dueAt = Rules.due(Kind.SERIES, ago(2.0))))
        container.settings.update { it.copy(onboarded = true, name = if (ru) "Саша" else "Alex") }
    }
}
