import Foundation
import Fluent
import Vapor

struct UserLibraryProcessor {
    var libStatus: LibraryProcessingStatus

    init(_ libStatus: LibraryProcessingStatus) {
        self.libStatus = libStatus
    }

    func process() async throws {
        do {
            let songs = try await parseSongsFromSpotify()

            // while(libStatus.processedItems < libStatus.totalItems) {
            //     try await Task.sleep(nanoseconds: 500000000)

            //     libStatus.processedItems += 1

                // if (libStatus.webSocket != nil) {
                //     try await libStatus.webSocket!.send("\(libStatus.progressPercentage)")
                // }
            // }

            // try await linkSpotifySongsToUser(songs: songs)
            // try await signalProcessingComplete()

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
                        spotifyId: item.track.id,
                    ))
                }

                libStatus.progressPercentage = (Double(songs.count) / Double(savedTracksResponse.total) * 50) / 100
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
            let user = try await User.find(libStatus.userId, on: libStatus.db)
            guard user != nil else {
                if (libStatus.webSocket != nil) { try await libStatus.webSocket!.close(code: .unexpectedServerError) }
                throw LyricaliaAPIError.inconsistency("NO USER FOR PROCESSED LIBRARY, I WILL DO ABSOLUTELY NOTHING ABOUT THIS!!!")
            }

            user!.isLibraryProcessed = true

            try await user!.save(on: libStatus.db)
        } catch {
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
        if (libStatus.webSocket != nil) {
            try await libStatus.webSocket!.close(code: .normalClosure)
        }
    }
}