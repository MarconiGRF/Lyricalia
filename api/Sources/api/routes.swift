import Vapor

func routes(_ app: Application) throws {
    try app.register(collection: UsersController())
    try app.register(collection: SpotifyController())
    try app.register(collection: MatchController())
}
