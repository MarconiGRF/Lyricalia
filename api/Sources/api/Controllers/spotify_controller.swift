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
        SpotifyAuthorizationRequest.getB64AuthString()

        spotifyRoutes.group("auth") { spotifyRoutes in
            spotifyRoutes.post(use: exchangeCode)
        }
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
