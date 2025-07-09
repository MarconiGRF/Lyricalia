import Fluent

final class MatchChallengeSet: Codable {
    var songs: [Song]
    var challenges: [String : [String]]

    init (_ songs: [Song], _ challenges: [String : [String]]) {
        self.songs = songs
        self.challenges = challenges
    }
}