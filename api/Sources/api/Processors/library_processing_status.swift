import Fluent
import Vapor

class LibraryProcessingStatus {
    var totalItems: Int
    var progressPercentage: Double
    let userId: UUID
    let spotifyToken: String
    var webSocket: WebSocket?
    var db: any Database
    var client: any Client

    init( _ userId: UUID, _ spotifyToken: String, _ db: any Database, _ client: any Client) {
        self.totalItems = 100
        self.progressPercentage = 0.0
        self.userId = userId
        self.spotifyToken = spotifyToken
        self.webSocket = nil
        self.db = db
        self.client = client
    }
}