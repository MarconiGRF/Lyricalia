package br.dev.marconi.lyricalia.utils

import android.content.Context
import android.util.Log
import br.dev.marconi.lyricalia.repositories.user.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class StorageUtils(private val filesDir: File) {
    fun deleteUser() {
        val file = File(filesDir, USER_FILENAME)
        file.delete()
        Log.d("IF1001_P3_LYRICALIA", "Deleted user from internal storage")
    }

    fun saveServerIp(serverIp: String) {
        val file = File(filesDir, SERVER_CONFIG_FILENAME)
        file.writeText(serverIp)
    }

    fun retrieveServerIp(): String {
        val file = File(filesDir, SERVER_CONFIG_FILENAME)
        return file.readText()
    }

    fun saveUser(user: User) {
        val jsonifiedUser = Json.encodeToString(user)
        val file = File(filesDir, USER_FILENAME)
        file.writeText(jsonifiedUser)
        Log.d("IF1001_P3_LYRICALIA", "Saved user ${user.username} to internal storage")
    }

    fun retrieveUser(): User? {
        try {
            val file = File(filesDir, USER_FILENAME)
            val jsonifiedUser = file.readText()
            val actualUser = Json.decodeFromString<User>(jsonifiedUser)
            return actualUser
        } catch (e: Exception) {
            Log.d("IF1001_P3_LYRICALIA", "Error retrieving user: " + e.cause.toString())
            return null
        }
    }

    companion object {
        const val USER_FILENAME = "user.json"
        const val SERVER_CONFIG_FILENAME = "server"
    }
}