package br.dev.marconi.lyricalia.repositories.lyric

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Lyric::class], version = 1)
abstract class LyricDatabase : RoomDatabase() {
    abstract fun lyricDao(): LyricDao
}