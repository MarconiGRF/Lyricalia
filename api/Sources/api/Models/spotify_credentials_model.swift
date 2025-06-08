import Vapor

final class SpotifyCredentials: Content, @unchecked Sendable {

    var id: Int?

    var authorizationCode: String

    var accessToken: String?

    var refreshToken: String?

    var expiresIn: Int?

    init(id: Int, authorizationCode: String, refreshToken: String? = nil, expiresIn: Int) {
        self.id = id
        self.authorizationCode = authorizationCode
        self.refreshToken = refreshToken
        self.expiresIn = expiresIn
    }
}
