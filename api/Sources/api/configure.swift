import Vapor
import Fluent
import FluentSQLiteDriver

// configures your application
public func configure(_ app: Application) async throws {
    // uncomment to serve files from /Public folder
    // app.middleware.use(FileMiddleware(publicDirectory: app.directory.publicDirectory))

    app.databases.use(.sqlite(.file("Sources/api/Persistence/db.sqlite")), as: .sqlite)
    app.migrations.add(UserMigration())

    try await app.autoMigrate()
    try routes(app)
}


// extension Request {
//     var userLibraryProcessorQueue: UserLibraryProcessorQueue? {
//         get {
//             self.storage[UserLibraryProcessorQueueKey.self]
//         }
//         set {
//             self.storage[UserLibraryProcessorQueueKey.self] = newValue
//         }
//     }
// }