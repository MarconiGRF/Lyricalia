package br.dev.marconi.lyricalia.repositories.login.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val username: String,
    val name: String,
    val id: String?,
    val spotifyToken: String?,
    val spotifyUserId: String?
) {
    constructor(username: String, name: String): this(username, name, null, null, null)
}
