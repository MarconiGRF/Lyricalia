package br.dev.marconi.lyricalia.repositories.spotifyCredentials

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SpotifyCredentialsDao {
    @Query("SELECT * FROM spotify_credentials WHERE user_id LIKE (:userId)")
    fun findById(userId: Int): SpotifyCredentialsEntity
}