package br.dev.marconi.lyricalia.repositories.match


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class PlayerPodium (
    val id: String,
    val score: Int,
    val submission: List<String>
)
