package br.dev.marconi.lyricalia.repositories.lyric

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
class Lyric (
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "timed_content") val timedContent: String?,
    @ColumnInfo(name = "has_syncing") val hasTiming: Boolean,
)