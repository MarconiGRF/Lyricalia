import Vapor

final class PlayerPodium: Codable {
    let id: UUID
    let score: Int
    let submission: [String]?

    init(id: UUID, score: Int, submission: [String]? = nil) {
        self.id = id
        self.score = score
        self.submission = submission
    }
}
