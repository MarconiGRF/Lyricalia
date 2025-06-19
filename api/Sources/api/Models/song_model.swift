import Fluent
import Vapor

final class Song: Model, @unchecked Sendable {
    static let schema = "songs"

    @ID(key: .id)
    var id: UUID?

    @Field(key: "name")
    var name: String

    @Field(key: "artist")
    var artist: String

    @Field(key: "spotifyId")
    var spotifyId: String

    @Siblings(through: UserSong.self, from: \.$song, to: \.$user)
    public var users: [User]

    init () { }

    init(
        id: UUID? = nil,
        name: String,
        artist: String,
        spotifyId: String
    ) {
        self.id = id
        self.name = name
        self.artist = artist
        self.spotifyId = spotifyId
    }
}