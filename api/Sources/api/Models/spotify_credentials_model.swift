import Vapor

final class SpotifyCredentials: Content, @unchecked Sendable {

    var id: Int?

    var authorizationCode: String

    var userId: String

    var accessToken: String?

    var refreshToken: String?

    var expiresIn: UInt64?

    init(id: Int, authorizationCode: String, userId: String, refreshToken: String? = nil, expiresIn: Int) {
        self.id = id
        self.authorizationCode = authorizationCode
        self.userId = userId
        self.refreshToken = refreshToken
    }
}
