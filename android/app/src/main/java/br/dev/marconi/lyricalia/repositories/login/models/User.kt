package br.dev.marconi.lyricalia.repositories.login.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
data class User (
    val username: String,
    val name: String,
    val id: String?,
    val spotifyToken: String?,
    val spotifyUserId: String?
): Parcelable {
    constructor(username: String, name: String): this(username, name, null, null, null)

    constructor(parcel: Parcel): this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(username)
        dest.writeString(name)
        dest.writeString(id)
        dest.writeString(spotifyUserId)
        dest.writeString(spotifyToken)
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}
