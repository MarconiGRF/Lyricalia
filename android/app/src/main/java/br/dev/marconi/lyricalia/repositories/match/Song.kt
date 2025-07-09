package br.dev.marconi.lyricalia.repositories.match

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val spotifyId: String
)