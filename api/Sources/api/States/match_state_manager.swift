import Vapor

class MatchStateManager: @unchecked Sendable {
    static let instance: MatchStateManager = { return MatchStateManager() }()

    private var state: [String : Match] = [:]

    private var artists = [
        "ImagineDragons",
        "Maneskin",
        "LadyGaga",
        "Beyonce",
        "TaylorSwift",
        "TaylorSwift",
    ]

    func create(songLimit: Int) -> String {
        let uuid = UUID().uuidString
        let match = Match(songLimit: songLimit)

        state[uuid] = match
        return uuid
    }
}
