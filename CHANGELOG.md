# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.3.0] - 2026-11-13

### Added
- A filter over the shelves on the main screen: one kind of box (films, series, anime, albums, books, comics, manga) and/or only what you haven't taken yet or rated ★ 4 and up. It stays at the top while you scroll.

### Changed
- The catalogue, the member card and the bin moved from the very bottom of the main screen to three tiles under "Bring in", so a long list no longer hides them.

<!-- tg
Новое: на главном экране появился фильтр полок – по виду (фильмы, сериалы, альбомы, книги…) и «ещё не брали» / «★ 4 и выше».
Картотека, абонемент и уценка теперь сразу под кнопками, а не в самом низу длинного списка.
-->

## [1.2.1] - 2026-11-13

### Fixed
- The two slots of "At home" are the same height, whatever kind of box is in them, and their titles line up.
- On every shelf, boxes of different kinds stand on one line and their titles sit on one line under them.

<!-- tg
Исправлено: карточки «На руках» одного размера, а названия на полках стоят на одном уровне.
-->

## [1.2.0] - 2026-11-13

### Added
- Delete a tape added by mistake: the trash can on the tape's screen removes it from the shop for good, with its slips, after a confirmation (#3).

<!-- tg
Новое: фильм, сериал, альбом или книгу, добавленные по ошибке, можно удалить – значок корзины на странице кассеты.
-->

## [1.1.1] - 2026-11-08

### Added
- Support Rewind: Settings → StillCraft has a fifth line, «Поддержать Прокат · на новые кассеты и перемотку», which opens the StillCraft support page in the browser.

<!-- tg
В настройках, в блоке StillCraft, появилась строчка «Поддержать Прокат» – на новые кассеты и перемотку.
-->

## [1.1.0] - 2026-11-08

### Added
- **Your own cover** (#1): on a tape's page, or in the form when bringing one in, pick a picture from the gallery through the system photo picker (no storage permission). It's scaled down and kept in the app, shown wherever the box stands instead of the catalogue's picture, and can be swapped or dropped to go back to the catalogue cover.
- **Anime and manga** (#2): two new kinds with their own aisles. Anime comes on a Japanese VHS with its obi and goes out for a week, like a series; manga is a tankōbon with a volume number and goes out for three days, like a comic. The catalogue finds them through Shikimori (with Russian titles), AniList and Wikidata, with Jikan (MyAnimeList) as a last resort, with the original title, episodes or volumes in the results; MyAnimeList, Shikimori and AniList links shared in land on the right shelf.

### Changed
- The kind picker in the form and the search wraps onto two lines, now that there are seven kinds.

<!-- tg
Свою обложку теперь можно поставить на любую кассету: выбери картинку из галереи на странице кассеты или при добавлении.
Новые полки: аниме и манга. Ищутся в Шикимори и AniList, аниме берут на неделю, мангу на три дня.
-->

## [1.0.1] - 2026-10-25

### Added
- Community links in settings: report a bug, suggest an idea or follow updates through the StillCraft bot on Telegram, and the StillCraft chat.

<!-- tg skip -->

## [1.0.0]

The first release.

- **A video rental store for your watchlist:** films on VHS, series as box sets, albums on audio cassettes, books from the library corner, comics from the spinner rack, face out on the shelves.
- **Bring in a tape** from the free catalogue (Wikidata and Wikipedia, TVmaze, iTunes) with its cover, year, maker and length; by *Share → Rewind* from Kinopoisk, IMDb, Letterboxd, Spotify, Yandex Music, Goodreads or LiveLib; or by hand. The back of the box is in your own words.
- **The window:** new tapes wait a few days behind the glass before reaching the shelf.
- **Two at a time,** with a receipt and a due date: two evenings for a film or an album, a week for a series, two weeks for a book, three days for a comic.
- **Be kind, rewind:** turn the reels (or hold REW) before handing a tape back, and write the return slip: three words, the moment that stayed, or why it didn't go.
- **Stars:** one to five on the return slip, changeable later; four and up make the store hits shelf.
- **The card catalogue:** an index card for every tape, sorted seven ways with divider cards, filtered by kind and by where the tape is.
- **Evening hours:** from 6 pm on weekdays and noon at the weekend, until 11; adjustable, or off.
- **Wrapped in brown paper:** the clerk picks a tape for the time you have, leaning toward what's waited longest.
- **The bargain bin:** untouched boxes fade in the sun; bleached ones can be marked down, rescued or thrown out.
- **Recommend to a friend** with a plain‑text card; shared back in, it lands on that friend's shelf.
- **The member card:** stamps for every return, the hit of the month, the year on a till roll.
- **Reminders** before a tape is due, when it's late, and when one leaves the window.
- English and Russian.
