import struct Foundation.UUID
import Vapor
import Fluent
import SQLKit

struct PlayerSubmission {
    var hasSubmitted: Bool = false
    var submission: String = ""
}

class PlayingUser {
    let user: User

    var isReady: Bool
    var ws: WebSocket
    var score: Int64 = 0
    var submission: [PlayerSubmission] = []

    init(_ user: User, _ ws: WebSocket) {
        self.user = user
        self.ws = ws
        self.isReady = false
    }
}

class Match: @unchecked Sendable {
    let songLimit: Int
    let db: any Database

    var allReady: Bool {
        get { players.compactMap{ $0.isReady }.reduce(true) { $0 && $1 } }
    }
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
                do { await removePlayer(playerId: removablePlayer) }
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

    func removePlayer(playerId: String) async {
        let id = UUID(uuidString: playerId)
        if (id != nil) {
            await removePlayer(playerId: id!)
        }
    }

    func removePlayer(playerId: UUID) async {
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

    func ackReadiness(playerId: String, ws: WebSocket) async {
        do {
            let player = players.first { $0.user.id!.uuidString == playerId }
            if (player == nil) { throw LyricaliaAPIError.inconsistency("No player found to be ready with id \(playerId)") }
            player!.isReady = true

            try await player!.ws.close(code: .normalClosure)
            player!.ws = ws

            if (allReady) {
                print("All players ready!")

                Task { try await process() }

                for player in players {
                    try await player.ws.send(MatchMessages.PROCESSING.rawValue)
                }
            } else {
                try await player!.ws.send(MatchMessages.WAITING.rawValue)
            }
        } catch {
            print("Failed to ack player readinesss \(error)")
        }
    }

    func process() async throws {
        print("Processing match...")

        do {
            let userIds = self.players.compactMap{ $0.user.id }

            let songs = try await (self.db as! any SQLDatabase).raw("""
                SELECT * FROM songs
                WHERE id IN (
                    SELECT song_id FROM 'users+songs'
                    WHERE user_id IN (\( self.players.compactMap{ "'\($0.user.id!.uuidString)'" }.joined(separator: ",") ))
                    GROUP BY song_id HAVING COUNT(DISTINCT user_id) = \(bind: userIds.count)
                )
                """)
                .all(decodingFluent: Song.self)

            
            songs.map{ print("\($0.artist) - \($0.name)") }
        } catch {
            print("Error processing match's songs -> \(error)")
        }
    }

    init(songLimit: Int, db: any Database) {
        self.songLimit = songLimit
        self.db = db
    }
}
