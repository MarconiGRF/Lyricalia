package br.dev.marconi.lyricalia.repositories.spotifyCredentials

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SpotifyCredentialsEntity::class], version = 1)
abstract class SpotifyCredentialsDatabase : RoomDatabase() {
    abstract fun spotifyCredentialsDao(): SpotifyCredentialsDao

    companion object {
        fun getInstance(context: Context): SpotifyCredentialsDatabase {
            return Room.databaseBuilder<SpotifyCredentialsDatabase>(
                context,
                SpotifyCredentialsDatabase::class.java, "lyric_db"
            ).build()
        }
    }
}