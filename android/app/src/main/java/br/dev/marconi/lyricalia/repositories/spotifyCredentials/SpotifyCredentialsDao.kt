package br.dev.marconi.lyricalia.repositories.spotifyCredentials

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SpotifyCredentialsDao {
    @Query("SELECT * FROM spotify_credentials LIMIT 1")
    fun findFirst(): SpotifyCredentialsEntity?

    @Insert
    fun insert(credentials: SpotifyCredentialsEntity)

    @Delete
    fun delete(credentials: SpotifyCredentialsEntity)
}