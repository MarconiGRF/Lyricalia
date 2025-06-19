import Vapor

final class SpotifySavedTrackResponse: Content, @unchecked Sendable {
    let href: String
    let limit: Int
    let next: String?
    let offset: Int
    let previous: String?
    let total: Int
    let items: [SpotifySavedTrack]
}

final class SpotifySavedTrack: Content, @unchecked Sendable {
    let added_at: String
    let track: SpotifyTrack
}

final class SpotifyTrack: Content, @unchecked Sendable {
    let album: SpotifyAlbum
    let artists: [SpotifySimplifiedArtist]
    let available_markets: [String]
    let disc_number: Int
    let duration_ms: Int
    let explicit: Bool
    let external_ids: SpotifyExternalIds
    let external_urls: SpotifyExternalURLs
    let href: String
    let id: String
    let is_playable: Bool
    let linked_from: SpotifyLinkedFrom?
    let restrictions: SpotifyRestrictions?
    let name: String
    let popularity: Int
    let preview_url: String?
    let track_number: Int
    let type: String
    let uri: String
    let is_local: Bool
}

final class SpotifyAlbum: Content, @unchecked Sendable {
    let album_type: String
    let total_tracks: Int
    let available_markets: [String]
    let external_urls: SpotifyExternalURLs
    let href: String
    let id: String
    let images: [SpotifyImage]
    let name: String
    let release_date: String
    let release_date_precision: String
    let restrictions: SpotifyRestrictions?
    let type: String
    let uri: String
    let artists: [SpotifySimplifiedArtist]
}

final class SpotifyImage: Content, @unchecked Sendable {
    let url: String
    let height: Int
    let width: Int
}

final class SpotifySimplifiedArtist: Content, @unchecked Sendable {
    let external_urls: SpotifyExternalURLs
    let href: String
    let id: String
    let name: String
    let type: String
    let uri: String
}

final class SpotifyExternalIds: Content, @unchecked Sendable {
    let isrc: String?
    let ean: String?
    let upc: String?
}

final class SpotifyExternalURLs: Content, @unchecked Sendable {
    let spotify: String
}

final class SpotifyLinkedFrom: Content, @unchecked Sendable { }

final class SpotifyRestrictions: Content, @unchecked Sendable {
    let reason: String
}