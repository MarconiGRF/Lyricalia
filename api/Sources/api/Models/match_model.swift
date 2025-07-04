import struct Foundation.UUID
import Vapor
import Fluent

struct PlayerSubmission {
    var hasSubmitted: Bool = false
    var submission: String = ""
}

struct PlayingUser {
    let ws: WebSocket
    let user: User
    var score: Int64 = 0
    var submission: [PlayerSubmission] = []

    init(_ user: User, _ ws: WebSocket) {
        self.user = user
        self.ws = ws
    }
}

class Match: @unchecked Sendable {
    let songLimit: Int

    var hostId: UUID?
    var commonSongs: [Song] = []
    var chosenSongs: [Song] = []
    var players: [PlayingUser] = [] {
        didSet { Task {
            var isHostIn = false
            players.compactMap { $0.user }.forEach {
                if ($0.id! == hostId) {
                    isHostIn = true
                    return
                }
            }

            if (!isHostIn) {
                for player in players {
                    do { try await player.ws.send(HostCommands.RECEIVABLE_END.rawValue) }
                }
            }
        }}
    }
    var inProgress: Bool = false {
        didSet { Task {
            print("Match progress changed! Notifying players")
            var removablePlayers: [UUID] = []

            for player in players {
                do { try await player.ws.send(
                        inProgress
                            ? HostCommands.SENDABLE_START.rawValue
                            : HostCommands.SENDABLE_END.rawValue
                    )
                }
                catch {
                    print("Could not communicate with player \(player.user.id!), assuming disconnected and removing.")
                    removablePlayers.append(player.user.id!)
                }
            }

            for removablePlayer in removablePlayers {
                do { try await removePlayer(playerId: removablePlayer) }
            }
        }}
    }

    func setHost(hostId: String) throws {
        guard let uuid = UUID(uuidString: hostId) else {
            throw LyricaliaAPIError.inconsistency("Invalid UUID to be set as host of match")
        }
        self.hostId = uuid
    }

    func addPlayer(playerId: String, ws: WebSocket, db: any Database) async throws {
        guard let uuid = UUID(uuidString: playerId) else {
            throw LyricaliaAPIError.inconsistency("Invalid UUID to be set as host of match")
        }
        guard let user = try await User.find(uuid, on: db) else {
            throw LyricaliaAPIError.inconsistency("No user found for given UUID to be set as host of match")
        }

        let playingUser = PlayingUser(user, ws)

        for player in players {
            let jsonifiedPlayer = try JSONEncoder().encode(
                JoinedPlayer(id: uuid, name: user.name, username: user.username)
            )
            do {
                try await player.ws.send(
                    PlayerMessages.JOINED.rawValue + String(data: jsonifiedPlayer, encoding: .utf8)!
                )
            }
        }

        players.append(playingUser)
        for player in players {
            let jsonifiedPlayer = try JSONEncoder().encode(
                JoinedPlayer(id: player.user.id!, name: player.user.name, username: player.user.username)
            )
            do {
                try await ws.send(
                    PlayerMessages.JOINED.rawValue + String(data: jsonifiedPlayer, encoding: .utf8)!
                )
            }
        }
    }

    func start() {
        inProgress = true
    }

    func end() {
        inProgress = false
    }

    func removePlayer(playerId: String) async throws {
        let id = UUID(uuidString: playerId)
        if (id != nil) {
            try await removePlayer(playerId: id!)
        }
    }

    func removePlayer(playerId: UUID) async throws {
        do {
            let playerIndex = players.firstIndex(where: { $0.user.id! == playerId })
            if playerIndex != nil {
                players.remove(at: playerIndex!)
            }

            for player in players {
                try await player.ws.send(
                    PlayerMessages.LEFT.rawValue + playerId.uuidString
                )
            }

        } catch {
            print("Failed to remove player \(playerId.uuidString) due to \(error)")
        }
    }

    init(songLimit: Int) {
        self.songLimit = songLimit
    }
}
