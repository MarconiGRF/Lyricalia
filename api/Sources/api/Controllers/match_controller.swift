import Vapor
import Fluent

struct MatchController: RouteCollection {
    func boot(routes: any RoutesBuilder) throws {
        let matchRoutes = routes.grouped("match")
        matchRoutes.post(use: create)

        matchRoutes.group("exists", ":match-id") { matchRoutes in
            matchRoutes.get(use: exists)
        }

        matchRoutes.group(":match-id") { matchRoutes in
            matchRoutes.webSocket{ req, ws in
                join(req, ws)
            }
        }
    }

    func exists(req: Request) async throws -> HTTPStatus {
        guard let matchId = req.parameters.get("match-id") else {
            throw Abort(.badRequest, reason: "Match ID is needed to check if match exists")
        }

        let matchExists = MatchStateManager.instance.exists(matchId)
        if matchExists { return .ok }
        else { return .notFound }
    }

    func create(req: Request) async throws -> String {
        let matchRequest = try req.content.decode(CreateMatchRequest.self)

        let matchUUID = MatchStateManager.instance.create(
            songLimit: matchRequest.songLimit,
            db: req.db,
            lyricsDB: req.db(.lyrics)
        )

        return matchUUID
    }

    func join(_ req: Request, _ ws: WebSocket) {
        let matchId = req.parameters.get("match-id")!

        ws.onText { ws, text in
            let command = text.components(separatedBy: "$")

            switch command[0] {
            case "host":
                handleHostCommand(command[1..<command.count], matchId, ws, req.db(.sqlite))

            case "player":
                handlePlayerMessage(command[1..<command.count], matchId, ws, req.db(.sqlite))

            default:
                print("???")
            }
        }
    }

    func handlePlayerMessage(
        _ message: ArraySlice<String>,
        _ matchId: String,
        _ ws: WebSocket,
        _ db: any Database
    ) {
        do {
            let match = try MatchStateManager.instance.get(matchId)

            switch message[1] {
                case PlayerMessages.JOIN.rawValue:
                    print("  -> Player joining")
                    Task { try await match.addPlayer(playerId: message[2], ws: ws, db: db) }

                case PlayerMessages.LEAVE.rawValue:
                    print("  -> Player leaving")
                    Task { await match.removePlayer(playerId: message[2]) }

                case PlayerMessages.READY.rawValue:
                    print("  -> Player ready")
                    Task { await match.ackReadiness(playerId: message[2], ws: ws) }

                case PlayerMessages.CHALLENGE_READY.rawValue:
                    print("  -> Player ready for challenge")
                    Task { await match.ackChallenge(playerId: message[2]) }

                case PlayerMessages.INPUT_READY.rawValue:
                    print("  -> Player ready for input")
                    Task { await match.ackInput(playerId: message[2]) }

                default:
                    throw LyricaliaAPIError.invalidCommand
            }
        } catch {
            print("  !!! Error processing PLAYER command \(message.joined(by: "$"))")
            print("    ---> \(error)")
        }
    }

    func handleHostCommand(
        _ command: ArraySlice<String>,
        _ matchId: String,
        _ ws: WebSocket,
        _ db: any Database
    ) {
        do {
            let match = try MatchStateManager.instance.get(matchId)

            switch command[1] {
                case HostCommands.RECEIVABLE_SET.rawValue:
                    print("  -> Setting host")
                    try match.setHost(hostId: command[2])
                    Task { try await match.addPlayer(playerId: command[2], ws: ws, db: db) }

                case HostCommands.RECEIVABLE_START.rawValue:
                    print("  -> Starting match")
                    match.start()

                case HostCommands.RECEIVABLE_END.rawValue:
                    print("  -> Ending match")
                    match.end()
                    MatchStateManager.instance.cease(matchId: matchId)

                default:
                    throw LyricaliaAPIError.invalidCommand
            }
        } catch {
            print("  !!! Error processing HOST command \(command.joined(by: "$"))")
            print("    ---> \(error)")
        }
    }
}