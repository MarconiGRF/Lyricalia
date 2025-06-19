import Vapor
import Foundation

final class SpotifyAuthorizationRequest: Content, @unchecked Sendable {
    private static let CLIENT_ID = "20a9c0b160ed45d88f8a3e0103924703"
    private static let CLIENT_SECRET = Environment.get("CLIENT_SECRET")

    var grant_type: String = "authorization_code"
    var code: String
    var redirect_uri: String = "lyricalia://link"

    init(_ code: String) {
        self.code = code
    }

    static func getB64AuthString() -> String {
        let clientInfo = "\(CLIENT_ID):\(CLIENT_SECRET!)"
        return "Basic \(clientInfo.data(using: .utf8)!.base64EncodedString())"
    }
}