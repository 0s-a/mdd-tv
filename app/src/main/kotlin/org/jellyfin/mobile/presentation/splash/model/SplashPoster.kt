package org.jellyfin.mobile.presentation.splash.model

import androidx.compose.runtime.Immutable

@Immutable
data class SplashPoster(
    val title: String,
    val badge: String,
    val imageUrl: String
)

val SplashPosterCatalog: List<SplashPoster> = listOf(
    SplashPoster(
        title = "Attack on Titan",
        badge = "EPIC ANIME",
        imageUrl = "https://image.tmdb.org/t/p/w780/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg"
    ),
    SplashPoster(
        title = "Spider-Man: Into the Spider-Verse",
        badge = "MARVEL ANIMATION",
        imageUrl = "https://image.tmdb.org/t/p/w780/iiZZdoQBEYBv6id8su7ImL0oCbD.jpg"
    ),
    SplashPoster(
        title = "Interstellar",
        badge = "SCI-FI CINEMA",
        imageUrl = "https://image.tmdb.org/t/p/w780/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"
    ),
    SplashPoster(
        title = "Demon Slayer",
        badge = "ANIME MASTERPIECE",
        imageUrl = "https://image.tmdb.org/t/p/w780/xUfRZu2mi8jH6SzQEJGP6tjBuYj.jpg"
    ),
    SplashPoster(
        title = "Avengers: Endgame",
        badge = "MARVEL STUDIOS",
        imageUrl = "https://image.tmdb.org/t/p/w780/or06FN3Dka5tukK1e9sl16pB3iy.jpg"
    ),
    SplashPoster(
        title = "The Dark Knight",
        badge = "DC CINEMATIC",
        imageUrl = "https://image.tmdb.org/t/p/w780/qJ2tW6WMUDux911r6m7haRef0WH.jpg"
    ),
    SplashPoster(
        title = "Breaking Bad",
        badge = "MASTERPIECE SERIES",
        imageUrl = "https://image.tmdb.org/t/p/w780/ggFHVNu6YYI5L9pCfOacjizRGt.jpg"
    ),
    SplashPoster(
        title = "Game of Thrones",
        badge = "HBO ORIGINAL",
        imageUrl = "https://image.tmdb.org/t/p/w780/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg"
    ),
    SplashPoster(
        title = "Spirited Away",
        badge = "GHIBLI CLASSIC",
        imageUrl = "https://image.tmdb.org/t/p/w780/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg"
    ),
    SplashPoster(
        title = "Dune",
        badge = "WARNER BROS",
        imageUrl = "https://image.tmdb.org/t/p/w780/d5NXSklXo0qyIYkgV94XAgMIckC.jpg"
    ),
    SplashPoster(
        title = "Fight Club",
        badge = "CULT CLASSIC",
        imageUrl = "https://image.tmdb.org/t/p/w780/bptfVGEQuv6vDTIMVCHjJ9Dz8PX.jpg"
    ),
    SplashPoster(
        title = "Pulp Fiction",
        badge = "CINEMA LEGEND",
        imageUrl = "https://image.tmdb.org/t/p/w780/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg"
    ),
    SplashPoster(
        title = "Gladiator",
        badge = "OSCAR WINNER",
        imageUrl = "https://image.tmdb.org/t/p/w780/ty8TGRuvJLPUmAR1H1nRIsgwvim.jpg"
    ),
    SplashPoster(
        title = "Your Name",
        badge = "MAKOTO SHINKAI",
        imageUrl = "https://image.tmdb.org/t/p/w780/q719jXXEzOoYaps6babgKnONONX.jpg"
    ),
    SplashPoster(
        title = "Avatar",
        badge = "JAMES CAMERON",
        imageUrl = "https://image.tmdb.org/t/p/w780/kyeqWdyUXW608qlYkRqosgbbJyK.jpg"
    ),
    SplashPoster(
        title = "Inception",
        badge = "CHRISTOPHER NOLAN",
        imageUrl = "https://image.tmdb.org/t/p/w780/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg"
    ),
    SplashPoster(
        title = "Oppenheimer",
        badge = "CHRISTOPHER NOLAN",
        imageUrl = "https://image.tmdb.org/t/p/w780/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg"
    ),
    SplashPoster(
        title = "The Shawshank Redemption",
        badge = "TIMELESS CLASSIC",
        imageUrl = "https://image.tmdb.org/t/p/w780/bQXAqRx2Fgc46uCVWgoPz5L5Dtr.jpg"
    ),
    SplashPoster(
        title = "Schindler's List",
        badge = "OSCAR WINNER",
        imageUrl = "https://image.tmdb.org/t/p/w780/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"
    ),
    SplashPoster(
        title = "Parasite",
        badge = "PALME D'OR",
        imageUrl = "https://image.tmdb.org/t/p/w780/w3LxiVYdWWRvEVdn5RYq6jIqkb1.jpg"
    )
)
