package br.dev.marconi.lyricalia.repositories.lyric

import androidx.room.Dao
import androidx.room.Query

@Dao
interface LyricDao {
    @Query("SELECT * FROM lyrics")
    fun getAll(): List<Lyric>
}