package app.rewind

import app.rewind.data.Catalog
import app.rewind.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Jikan answers read into drafts (#2). Robolectric, for a real org.json. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JikanTest {
    @Test fun anime() {
        val body = """
            {"pagination":{},"data":[
              {"mal_id":1,"url":"https://myanimelist.net/anime/1/Cowboy_Bebop",
               "images":{"jpg":{"image_url":"https://cdn.myanimelist.net/images/anime/4/19644.jpg","large_image_url":"https://cdn.myanimelist.net/images/anime/4/19644l.jpg"}},
               "title":"Cowboy Bebop","title_english":"Cowboy Bebop","title_japanese":"カウボーイビバップ",
               "type":"TV","episodes":26,"duration":"24 min per ep","year":1998,
               "aired":{"from":"1998-04-03T00:00:00+00:00"},"studios":[{"mal_id":14,"name":"Sunrise"}]},
              {"mal_id":5114,"url":"https://myanimelist.net/anime/5114",
               "images":{"jpg":{"image_url":"https://example.org/a.jpg","large_image_url":null}},
               "title":"Hagane no Renkinjutsushi","title_english":null,"title_japanese":"鋼の錬金術師",
               "type":"Movie","episodes":null,"duration":"1 hr 45 min","year":null,
               "aired":{"from":"2009-04-05T00:00:00+00:00"},"studios":[]},
              {"mal_id":3,"title":"","title_english":null}
            ]}
        """.trimIndent()
        val (bebop, fma) = Catalog.jikanParse(body, Kind.ANIME).also { assertEquals(2, it.size) }
        assertEquals(Kind.ANIME, bebop.kind)
        assertEquals("Cowboy Bebop", bebop.title)
        assertEquals("カウボーイビバップ", bebop.original)
        assertEquals("Sunrise", bebop.creator)
        assertEquals(1998, bebop.year)
        assertEquals(24, bebop.minutes)
        assertEquals(26, bebop.episodes)
        assertEquals("https://myanimelist.net/anime/1/Cowboy_Bebop", bebop.link)
        assertEquals("https://cdn.myanimelist.net/images/anime/4/19644l.jpg", bebop.coverUrl)

        assertEquals("Hagane no Renkinjutsushi", fma.title)
        assertEquals(2009, fma.year)
        assertEquals(105, fma.minutes)
        assertNull(fma.episodes)
        assertEquals("", fma.creator)
        assertEquals("https://example.org/a.jpg", fma.coverUrl)
    }

    @Test fun manga() {
        val body = """
            {"data":[
              {"mal_id":2,"url":"https://myanimelist.net/manga/2/Berserk",
               "images":{"jpg":{"large_image_url":"https://cdn.myanimelist.net/images/manga/1/157897l.jpg"}},
               "title":"Berserk","title_english":"Berserk","title_japanese":"ベルセルク",
               "chapters":null,"volumes":null,"published":{"from":"1989-08-25T00:00:00+00:00"},
               "authors":[{"name":"Miura, Kentarou"},{"name":"Studio Gaga"}]},
              {"mal_id":104,"url":"https://myanimelist.net/manga/104/Yotsuba_to",
               "images":{"jpg":{"large_image_url":"https://cdn.myanimelist.net/images/manga/5/1.jpg"}},
               "title":"Yotsuba to!","title_english":"Yotsuba&!","title_japanese":"よつばと！",
               "chapters":120,"volumes":15,"published":{"from":"2003-03-21T00:00:00+00:00"},
               "authors":[{"name":"Azuma, Kiyohiko"}]}
            ]}
        """.trimIndent()
        val (berserk, yotsuba) = Catalog.jikanParse(body, Kind.MANGA)
        assertEquals(Kind.MANGA, berserk.kind)
        assertEquals("Kentarou Miura, Studio Gaga", berserk.creator)
        assertEquals(1989, berserk.year)
        assertNull(berserk.minutes)
        assertNull(berserk.volumes)
        assertEquals("Yotsuba&!", yotsuba.title)
        assertEquals("Yotsuba to!", yotsuba.original)
        assertEquals(15, yotsuba.volumes)
        assertEquals(120, yotsuba.chapters)
        assertNull(yotsuba.episodes)
    }

    @Test fun nothing() {
        assertEquals(0, Catalog.jikanParse("""{"data":[]}""", Kind.ANIME).size)
        assertEquals(0, Catalog.jikanParse("""{"status":429}""", Kind.MANGA).size)
    }
}
