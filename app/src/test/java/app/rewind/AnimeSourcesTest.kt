package app.rewind

import app.rewind.data.Catalog
import app.rewind.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Shikimori and AniList answers read into drafts (#2). Robolectric, for a real org.json. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnimeSourcesTest {
    private val shikiAnime = """
        [{"id":1,"name":"Cowboy Bebop","russian":"Ковбой Бибоп",
          "image":{"original":"/system/animes/original/1.jpg?1674378220","preview":"/system/animes/preview/1.jpg"},
          "url":"/animes/1-cowboy-bebop","kind":"tv","score":"8.75","status":"released",
          "episodes":26,"episodes_aired":0,"aired_on":"1998-04-03","released_on":"1999-04-24"},
         {"id":2,"name":"One Piece","russian":"",
          "image":{"original":"/assets/globals/missing_original.jpg"},
          "url":"/animes/21-one-piece","kind":"tv","episodes":0,"episodes_aired":1100,"aired_on":"1999-10-20","released_on":null}]
    """.trimIndent()

    @Test fun shikimoriInRussian() {
        val (bebop, piece) = Catalog.shikimoriParse(shikiAnime, Kind.ANIME, ru = true)
        assertEquals("Ковбой Бибоп", bebop.title)
        assertEquals("Cowboy Bebop", bebop.original)
        assertEquals(1998, bebop.year)
        assertEquals(26, bebop.episodes)
        assertEquals("https://shikimori.io/system/animes/original/1.jpg?1674378220", bebop.coverUrl)
        assertEquals("https://shikimori.io/animes/1-cowboy-bebop", bebop.link)
        // No Russian title: the other one; no picture rather than Shikimori's placeholder.
        assertEquals("One Piece", piece.title)
        assertNull(piece.original)
        assertNull(piece.coverUrl)
        assertEquals(1100, piece.episodes)
    }

    @Test fun shikimoriInEnglish() {
        val bebop = Catalog.shikimoriParse(shikiAnime, Kind.ANIME, ru = false).first()
        assertEquals("Cowboy Bebop", bebop.title)
        assertEquals("Ковбой Бибоп", bebop.original)
    }

    @Test fun shikimoriManga() {
        val body = """
            [{"id":2,"name":"Berserk","russian":"Берсерк","image":{"original":"/system/mangas/original/2.jpg"},
              "url":"/mangas/2-berserk","kind":"manga","volumes":0,"chapters":0,"aired_on":"1989-08-25","released_on":null},
             {"id":3,"name":"Spice and Wolf","russian":"Волчица и пряности","image":{"original":"/x.jpg"},
              "url":"/ranobe/3","kind":"light_novel","volumes":17,"chapters":0,"aired_on":"2006-02-10"}]
        """.trimIndent()
        val list = Catalog.shikimoriParse(body, Kind.MANGA, ru = true)
        assertEquals(1, list.size)
        assertEquals("Берсерк", list[0].title)
        assertEquals(1989, list[0].year)
        assertNull(list[0].volumes)
        assertNull(list[0].episodes)
    }

    @Test fun anilist() {
        val body = """
            {"data":{"Page":{"media":[
              {"title":{"romaji":"Cowboy Bebop","english":"Cowboy Bebop","native":"カウボーイビバップ"},
               "startDate":{"year":1998},"coverImage":{"large":"https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx1.jpg"},
               "siteUrl":"https://anilist.co/anime/1","episodes":26,"duration":24,"chapters":null,"volumes":null,
               "studios":{"nodes":[{"name":"Sunrise"}]},"staff":{"nodes":[{"name":{"full":"Shinichirou Watanabe"}}]}},
              {"title":{"romaji":"Shingeki no Kyojin","english":null,"native":"進撃の巨人"},
               "startDate":{"year":null},"coverImage":{"large":null},"siteUrl":"https://anilist.co/anime/16498",
               "episodes":null,"duration":null,"studios":{"nodes":[]},"staff":{"nodes":[]}}
            ]}}}
        """.trimIndent()
        val (bebop, aot) = Catalog.anilistParse(body, Kind.ANIME)
        assertEquals("Cowboy Bebop", bebop.title)
        assertEquals("カウボーイビバップ", bebop.original)
        assertEquals("Sunrise", bebop.creator)
        assertEquals(1998, bebop.year)
        assertEquals(24, bebop.minutes)
        assertEquals(26, bebop.episodes)
        assertEquals("https://anilist.co/anime/1", bebop.link)
        assertEquals("Shingeki no Kyojin", aot.title)
        assertEquals("進撃の巨人", aot.original)
        assertNull(aot.year)
        assertNull(aot.coverUrl)
        assertNull(aot.episodes)
    }

    @Test fun anilistManga() {
        val body = """
            {"data":{"Page":{"media":[
              {"title":{"romaji":"Yotsuba to!","english":"Yotsuba&!","native":"よつばと！"},"startDate":{"year":2003},
               "coverImage":{"large":"https://example.org/y.jpg"},"siteUrl":"https://anilist.co/manga/30104",
               "episodes":null,"duration":null,"chapters":null,"volumes":15,
               "studios":{"nodes":[]},"staff":{"nodes":[{"name":{"full":"Kiyohiko Azuma"}}]}}
            ]}}}
        """.trimIndent()
        val y = Catalog.anilistParse(body, Kind.MANGA).single()
        assertEquals("Yotsuba&!", y.title)
        assertEquals("Yotsuba to!", y.original)
        assertEquals("Kiyohiko Azuma", y.creator)
        assertEquals(15, y.volumes)
        assertNull(y.chapters)
        assertNull(y.minutes)
        assertEquals(0, Catalog.anilistParse("""{"errors":[{"message":"Too Many Requests"}],"data":null}""", Kind.ANIME).size)
    }
}
