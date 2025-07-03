import Vapor

final class JoinedPlayer: Content, @unchecked Sendable {
    var id: UUID
    var name: String
    var username: String

    init(id: UUID, name: String, username: String) {
        self.id = id
        self.name = name
        self.username = username
    }
}