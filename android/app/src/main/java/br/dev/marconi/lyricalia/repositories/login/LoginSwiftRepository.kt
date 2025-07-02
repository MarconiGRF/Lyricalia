package br.dev.marconi.lyricalia.repositories.login

import android.content.Context
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.StorageUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginSwiftRepository {
    private var serverIp: String

    constructor(context: Context) {
        this.serverIp = StorageUtils(context.filesDir).retrieveServerIp()
    }

    suspend fun createUser(name: String, username: String): User {
        val retrofit = Retrofit.Builder()
            .baseUrl(serverIp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create<LoginService>(LoginService::class.java)

        val user = User(name, username)
        val response = service.createUser(user)

        if (response.isSuccessful) {
            return response.body()!!
        } else {
            throw Error("Status ${response.code()}")
        }
    }
}