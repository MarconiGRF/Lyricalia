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

        spotifyRoutes.group("auth") { spotifyRoutes in
            spotifyRoutes.post(use: exchangeCode)
        }

        spotifyRoutes.group("library") { spotifyRoutes in
            spotifyRoutes.post(use: dispatchProcessUserLibrary)
            spotifyRoutes.webSocket { req, ws in getLibraryStatus(req, ws) }
        }
    }

    func getLibraryStatus(_ req: Request, _ ws: WebSocket) {
        ws.onText { ws, text in
            let uuid = UUID(uuidString: text)
            if (uuid == nil) { ws.close(code: .unacceptableData, promise: nil) }

            let queue = await UserLibraryProcessorQueue.shared

            let libStatus = queue.getStatusForUserID(userId: uuid!)
            if (libStatus == nil) { ws.close(code: .protocolError, promise: nil) }

            libStatus!.webSocket = ws
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
        }

        let libStatus = LibraryProcessingStatus(100, userId, spotifyToken)
        queue.append(libStatus)
        UserLibraryProcessor(libStatus: libStatus).start()

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
