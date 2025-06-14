import Fluent
import Vapor

class LibraryProcessingStatus {
    let totalItems: Int
    var processedItems: Int
    let userId: UUID
    let spotifyToken: String
    var webSocket: WebSocket?
    var db: any Database
    var client: any Client

    init(_ totalItems: Int, _ userId: UUID, _ spotifyToken: String, _ db: any Database, _ client: any Client) {
        self.totalItems = totalItems
        self.processedItems = 0
        self.userId = userId
        self.spotifyToken = spotifyToken
        self.webSocket = nil
        self.db = db
        self.client = client
    }
}