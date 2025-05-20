package br.dev.marconi.lyricalia.repositories.login

interface LoginRepository {
    suspend fun createUser(name: String, username: String)
}
