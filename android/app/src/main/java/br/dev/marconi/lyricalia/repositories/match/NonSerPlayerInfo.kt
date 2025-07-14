package br.dev.marconi.lyricalia.repositories.match


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NonSerPlayerInfo (
    val id: String,
    val name: String,
    val username: String
) : Parcelable
