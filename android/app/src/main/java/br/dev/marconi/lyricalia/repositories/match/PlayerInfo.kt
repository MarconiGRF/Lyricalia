package br.dev.marconi.lyricalia.repositories.match

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInfo (
    val id: String,
    val name: String,
    val userName: String
)