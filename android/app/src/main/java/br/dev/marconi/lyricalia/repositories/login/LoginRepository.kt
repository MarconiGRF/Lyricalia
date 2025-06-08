package br.dev.marconi.lyricalia.repositories.login

import br.dev.marconi.lyricalia.repositories.user.User

interface LoginRepository {
    suspend fun createUser(name: String, username: String): User
}
