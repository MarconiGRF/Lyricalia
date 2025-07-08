import Fluent
import Vapor

final class Lyric: Model, @unchecked Sendable {
    static let schema = "lyrics"

    var id: Int?

    @Field(key: "plain_lyrics")
    var plainLyrics: String

    @Field(key: "synced_lyrics")
    var syncedLyrics: String?

    @Field(key: "track_id")
    var trackId: Int

    init () { }
}