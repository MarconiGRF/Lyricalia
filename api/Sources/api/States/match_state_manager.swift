import Vapor
import Fluent

class MatchStateManager: @unchecked Sendable {
    static let instance: MatchStateManager = { return MatchStateManager() }()

    private var matches: [String : Match] = [:]

    private var artists = [
        "ImagineDragons",
        "Maneskin",
        "Gaga",
        "Beyonce",
        "TaylorSwift",
        "Foals",
        "Lana",
        "Lorde",
        "Muse",
        "OliviaRod",
        "SabrinaCarp",
        "WolfAlice"
    ]
    private var adjectives = [
        "IsAwesome",
        "IsCrazy",
        "Legend",
        "Flop",
        "Sad",
        "GlobalHit",
        "NotForMe",
        "Monarchy",
        "Dinasty",
        "Republic",
    ]

    func create(songLimit: Int, db: any Database) -> String {
        let match = Match(songLimit: songLimit, db: db)
        let id = "\(artists.randomElement()!)\(adjectives.randomElement()!)"

        matches[id] = match
        return id
    }

    func cease(matchId: String) {
        matches.removeValue(forKey: matchId)
    }

    func get(_ matchId: String) throws -> Match {
        guard let match = matches[matchId] else {
            throw LyricaliaAPIError.inconsistency("Match not Found")
        }

        return match
    }

    func exists(_ matchId: String) -> Bool { matches[matchId] != nil }
}
