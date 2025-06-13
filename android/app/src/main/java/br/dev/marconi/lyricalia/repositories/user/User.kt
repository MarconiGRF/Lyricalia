package br.dev.marconi.lyricalia.repositories.user

import kotlinx.serialization.Serializable

@Serializable
data class User (
    val name: String,
    val username: String,
    val id: String?,
    var spotifyToken: String?,
    var isLibraryProcessed: Boolean = false
) {
    constructor(name: String, username: String) : this(name, username, null, null, false)
}