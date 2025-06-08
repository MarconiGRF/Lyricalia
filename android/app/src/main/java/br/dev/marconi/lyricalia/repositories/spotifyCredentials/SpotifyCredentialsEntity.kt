package br.dev.marconi.lyricalia.repositories.spotifyCredentials

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "spotify_credentials")
class SpotifyCredentialsEntity (
    @ColumnInfo(name = "authorization_code") val authorizationCode: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "access_token") val accessToken: String? = null,
    @ColumnInfo(name = "refresh_token") val refreshToken: String? = null,
    @ColumnInfo(name = "expires_in") val expiresIn: Long? = null,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}