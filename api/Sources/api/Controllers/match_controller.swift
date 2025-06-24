import Vapor

struct MatchController: RouteCollection {
    func boot(routes: any RoutesBuilder) throws {
        let matchRoutes = routes.grouped("match")

        matchRoutes.post(use: create)

        matchRoutes.group(":match-id") { matchRoutes in
            matchRoutes.webSocket{ req, ws in
                join(req, ws)
            }
        }
    }

    func create(req: Request) async throws -> String {
        let matchRequest = try req.content.decode(CreateMatchRequest.self)

        let matchUUID = MatchStateManager.instance.create(songLimit: matchRequest.songLimit)

        return matchUUID
    }

    func join(_ req: Request, _ ws: WebSocket) {
        ws.onText { ws, text in
            switch text {
            case "host":
                print("host joined")

            case "player":
                print("player joined")

            default:
                print("???")

            }
        }
    }
}