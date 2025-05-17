package br.dev.marconi.lyricalia.repositories.login

interface LoginRepository {
    suspend fun createUser(name: String, userName: String)
}