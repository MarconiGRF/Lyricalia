//
//  spotify_controller.swift
//  api
//
//  Created by Marconi Filho on 03/06/25.
//
import Vapor

struct SpotifyController: RouteCollection {
    func boot(routes: any RoutesBuilder) throws {
        let spotifyRoutes = routes.grouped("spotify")

        spotifyRoutes.get(use: pi)

        spotifyRoutes.group("auth") { spotifyRoutes in
            spotifyRoutes.post(use: exchangeCode)
        }

        spotifyRoutes.group("library") { spotifyRoutes in
            spotifyRoutes.post(use: dispatchProcessUserLibrary)
            spotifyRoutes.webSocket { req, ws in getLibraryStatus(req, ws) }
        }
    }


    func pi(_ req: Request) async throws -> Bool {
        let spotifyResponse = try await req.client.get("https://api.spotify.com/v1/me/tracks?limit=50") { spotifyRequest in
            spotifyRequest.headers.add(name: .authorization, value: "Bearer ")
        }
        let e = try spotifyResponse.content.decode(SpotifySavedTrackResponse.self)
        return true
    }

    @Sendable
    func getLibraryStatus(_ req: Request, _ ws: WebSocket) {
        ws.onText { ws, text in
            let userId = UUID(uuidString: text)
            if (userId == nil) {
                ws.close(code: .unacceptableData, promise: nil)
                return
            }

            do {
                let maybeUser = try await User.find(userId, on: req.db)
                guard let user = maybeUser else {
                    ws.close(code: .unacceptableData, promise: nil)
                    return
                }
                if (user.isLibraryProcessed) {
                    ws.close(code: .normalClosure, promise: nil)
                    return
                }
            } catch {
                ws.close(code: .unexpectedServerError, promise: nil)
                return
            }

            let queue = await UserLibraryProcessorQueue.shared

            let libStatus = queue.getStatusForUserID(userId: userId!)
            if (libStatus == nil) {
                ws.close(code: .protocolError, promise: nil)
                return
            } else {
                Task {
                    libStatus!.webSocket = ws
                }
            }
        }
    }

    func dispatchProcessUserLibrary(req: Request) async throws -> Bool {
        let user = try req.content.decode(User.self)
        guard let userId = user.id else {
            throw Abort(.badRequest, reason: "User ID is required to process the user's library")
        }
        guard let spotifyToken = user.spotifyToken else {
            throw Abort(.badRequest, reason: "Spotify Token is required to process the user's library")
        }

        let queue = await UserLibraryProcessorQueue.shared
        if (queue.alreadyExists(userId: userId)) {
            throw Abort(.badRequest, reason: "User library already being processed")
            return false
        } else {
            let libStatus = LibraryProcessingStatus(userId, spotifyToken, req.db, req.client)
            queue.append(libStatus)

            Task {
                do {
                    try await UserLibraryProcessor(libStatus).process()
                } catch {
                    print("Error inside library processing task! \(error)")
                }
            }
        }

        return true
    }


    func exchangeCode(req: Request) async throws -> SpotifyCredentials {
        let credentials = try req.content.decode(SpotifyCredentials.self)
        let authResponse = try await talkToSpotify(req, credentials)

        credentials.accessToken = authResponse.access_token
        credentials.expiresIn = authResponse.expires_in
        credentials.refreshToken = authResponse.refresh_token
        return credentials
    }

    private func talkToSpotify(_ req: Request, _ creds: SpotifyCredentials) async throws -> SpotifyAuthorizationResponse {
        return try await req.client.post("https://accounts.spotify.com/api/token") { spotifyRequest in
            let b64EncodedClientInfo = SpotifyAuthorizationRequest.getB64AuthString()

            spotifyRequest.headers.add(name: .contentType, value: "application/x-www-form-urlencoded")
            spotifyRequest.headers.add(name: .authorization, value: b64EncodedClientInfo)
            try spotifyRequest.content.encode(
                SpotifyAuthorizationRequest(creds.authorizationCode),
                as: .urlEncodedForm
            )
        }.content.decode(SpotifyAuthorizationResponse.self)
    }
}
