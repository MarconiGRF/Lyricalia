import Vapor

final class PlayerPodium: Codable {
    let id: UUID
    let score: Int

    init(id: UUID, score: Int) {
        self.id = id
        self.score = score
    }
}
