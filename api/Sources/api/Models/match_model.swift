import struct Foundation.UUID
import Vapor

struct PlayerSubmission {
    var hasSubmitted: Bool = false
    var submission: String = ""
}

struct PlayingUser {
    var score: Int64 = 0
    var ws: WebSocket
    var user: User
    var submission: PlayerSubmission = PlayerSubmission()

    init(_ user: User, _ ws: WebSocket) {
        self.user = user
        self.ws = ws
    }
}

class Match: @unchecked Sendable {
    var players: [PlayingUser] = []
    var commonSongs: [Song] = []
    var chosenSongs: [Song] = []
    var hostId: UUID?
    var songLimit: Int
    var inProgress: Bool = false {
        didSet { Task {
            for player in players {
                do { try await player.ws.send("inProgress:\(inProgress)") }
                catch {
                    print("Could not communicate with player \(player.user.id!), assuming disconnected and removing.")
                    // removePlayer(playerUUID: player.user.id!)
                }
            }
        }}
    }

    private func removePlayer(playerUUID: UUID) {
        let playerIndex = players.firstIndex(where: { $0.user.id! == playerUUID })
        if playerIndex != nil {
            players.remove(at: playerIndex!)
        }
    }

    init(songLimit: Int) {
        self.songLimit = songLimit
    }
}
