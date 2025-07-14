package br.dev.marconi.lyricalia.repositories.match


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PlayerInfo (
    val id: String,
    val name: String,
    val username: String
) : Parcelable { }

fun PlayerInfo.toNonSer(): NonSerPlayerInfo {
    return NonSerPlayerInfo(this.id, this.name, this.username)
}