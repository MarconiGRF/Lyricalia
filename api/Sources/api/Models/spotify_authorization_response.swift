import Vapor

final class SpotifyAuthorizationResponse: Content, @unchecked Sendable {
    let access_token: String
    let token_type: String
    let scope: String
    let expires_in: Int
    let refresh_token: String
}