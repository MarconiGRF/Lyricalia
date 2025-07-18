package br.dev.marconi.lyricalia.repositories.match

import kotlinx.serialization.Serializable

@Serializable
data class MatchChallengeSet(
    val songs: List<Song>,
    val challenges: Map<String, MutableList<String>>
)