package br.dev.marconi.lyricalia.repositories.match

import kotlinx.serialization.Serializable

@Serializable
data class CreateMatchRequest (
    val songLimit: Int
)