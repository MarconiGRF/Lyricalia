import Vapor

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
        "OliviaRodrigo",
        "SabrinaCarpenter"
    ]
    private var adjectives = [
        "IsAwesome",
        "IsCrazy",
        "Legendary",
        "Flop",
        "Sad",
        "GlobalHit",
        "NotForMe",
        "Monarchy",
        "Dinasty",
        "Repubic",
    ]

    func create(songLimit: Int) -> String {
        let match = Match(songLimit: songLimit)
        let id = "\(artists.randomElement()!)\(adjectives.randomElement()!)"

        matches[id] = match
        return id
    }
}
