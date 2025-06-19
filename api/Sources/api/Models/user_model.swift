import Fluent
import Vapor

final class User: Model, Content, @unchecked Sendable {
    static let schema = "users"

    @ID(key: .id)
    var id: UUID?

    @Field(key: "name")
    var name: String

    @Field(key: "username")
    var username: String

    @Field(key: "spotifyToken")
    var spotifyToken: String?

    @Field(key: "isLibraryProcessed")
    var isLibraryProcessed: Bool

    @Siblings(through: UserSong.self, from: \.$user, to: \.$song)
    public var songss: [Song]

    init() {}

    init(username: String, name: String) {
        self.username = username
        self.name = name
        self.isLibraryProcessed = false
    }
}
