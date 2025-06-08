import Fluent
import Vapor

class LibraryProcessingStatus {
    let totalItems: Int
    var processedItems: Int
    let userId: UUID
    let spotifyToken: String
    var webSocket: WebSocket?

    init(_ totalItems: Int, _ userId: UUID, _ spotifyToken: String) {
        self.totalItems = totalItems
        self.processedItems = 0
        self.userId = userId
        self.spotifyToken = spotifyToken
        self.webSocket = nil
    }
}