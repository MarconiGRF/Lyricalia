import Foundation
import struct Foundation.UUID
import Fluent
import Vapor

struct UserLibraryProcessor: @unchecked Sendable {
    var libStatus: LibraryProcessingStatus

    init(_ libStatus: LibraryProcessingStatus) {
        self.libStatus = libStatus
    }

    func process() async throws {
        do {
            let songs = try await parseSongsFromSpotify()

            try await linkSpotifySongsToUser(songs: songs)
            try await signalProcessingComplete()

            await removeProcessingStatusFromQueue()
        } catch {
            if (libStatus.webSocket != nil) { try await libStatus.webSocket!.close(code: .unexpectedServerError) }
            libStatus.webSocket = nil
            print("Something went wrong when importing library from spotify for user \(libStatus.userId) -> \(error)")
        }
    }

    private func parseSongsFromSpotify() async throws -> [Song] {
        var songs: [Song] = []
        var songLimit = libStatus.totalItems
        var requestUrl: URI = "https://api.spotify.com/v1/me/tracks?limit=50"

        while (songs.count < songLimit) {
            do {
                let spotifyResponse = try await libStatus.client.get(requestUrl) { spotifyRequest in
                    spotifyRequest.headers.add(name: .authorization, value: "Bearer \(libStatus.spotifyToken)")
                }

                let savedTracksResponse = try spotifyResponse.content.decode(SpotifySavedTrackResponse.self)
                savedTracksResponse.items.forEach { item in
                    songs.append(Song(
                        name: item.track.name,
                        artist: item.track.artists[0].name,
                        spotifyId: item.track.id
                    ))
                }

                libStatus.progressPercentage = (Double(songs.count) / Double(savedTracksResponse.total) * 50) / 100
                print("\(libStatus.progressPercentage)% Progress for user \(libStatus.userId)")
                try await sendMessage(message: "\(libStatus.progressPercentage)")

                if (songLimit < savedTracksResponse.total) {
                    songLimit = savedTracksResponse.total
                    libStatus.totalItems = savedTracksResponse.total
                }

                guard let nextBatch = savedTracksResponse.next else {
                    break
                }
                requestUrl = URI(string: nextBatch)
            } catch {
                print("Error parsing user library -> \(error)")
                throw error
            }
        }

        return songs
    }

    private func linkSpotifySongsToUser(songs: [Song]) async throws {
        do {
            guard let user = try await User.find(libStatus.userId, on: libStatus.db) else {
                if (libStatus.webSocket != nil) { try await libStatus.webSocket!.close(code: .unexpectedServerError) }
                throw LyricaliaAPIError.inconsistency("NO USER FOR PROCESSED LIBRARY, I WILL DO ABSOLUTELY NOTHING ABOUT THIS!!!")
            }

            var existingSongIds: [String : Song] = [:]
            try await Song.query(on: libStatus.db)
                .with(\.$users)
                .filter(\.$spotifyId ~~ songs.map{ $0.spotifyId })
                .all()
                .forEach { existingSong in
                    existingSongIds[existingSong.spotifyId] = existingSong
                }

            libStatus.progressPercentage += 0.15
            print("\(libStatus.progressPercentage * 100)% Progress for user \(libStatus.userId)")
            try await sendMessage(message: "\(libStatus.progressPercentage)")

            for song in songs {
                if (existingSongIds[song.spotifyId] == nil) {
                    print("Song \(song.spotifyId) not found on DB, creating...")
                    try await song.create(on: libStatus.db)

                    print("Song \(song.spotifyId) created, attaching user...")
                    try await song.$users.attach(user, on: libStatus.db)
                } else {
                    print("Song \(song.spotifyId) exists, attaching user...")

                    let existingSong = existingSongIds[song.spotifyId]!
                    try await existingSong.$users.attach(user, method: .ifNotExists, on: libStatus.db)
                    try await existingSong.update(on: libStatus.db)
                }
            }

            libStatus.progressPercentage += 0.30
            print("\(libStatus.progressPercentage * 100)% Progress for user \(libStatus.userId)")
            try await sendMessage(message: "\(libStatus.progressPercentage)")

            user.isLibraryProcessed = true
            try await user.save(on: libStatus.db)
        } catch {
            print("Failed to Link user to songs -> \(error)")
            throw error
        }
    }

    private func sendMessage(message: String) async throws {
        do {
            if (libStatus.webSocket != nil) {
                try await libStatus.webSocket!.send(message)
            }
        } catch {
            print("Failed to communicate over websocket, it is invalid or closed, removing reference and continuing...")
            libStatus.webSocket = nil
        }
    }

    private func signalProcessingComplete() async throws {
        libStatus.progressPercentage = 1
        print("\(libStatus.progressPercentage)% Progress for user \(libStatus.userId)")
        try await sendMessage(message: "\(libStatus.progressPercentage)")

        if (libStatus.webSocket != nil) {
            try await libStatus.webSocket!.close(code: .normalClosure)
        }
    }

    private func removeProcessingStatusFromQueue() async {
        let queue = await UserLibraryProcessorQueue.shared
        queue.remove(userId: libStatus.userId)
    }
}