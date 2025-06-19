import Fluent
import Vapor

final class UserSong: Model, @unchecked Sendable {
    static let schema = "users+songs"

    @ID(key: .id)
    var id: UUID?

    @Parent(key: "user_id")
    var user: User

    @Parent(key: "song_id")
    var song: Song

    init () { }

    init(
        id: UUID? = nil,
        user: User,
        song: Song
    ) throws {
        self.id = id
        self.$user.id = try user.requireID()
        self.$song.id = try song.requireID()
    }
}