import Vapor
import Fluent
import FluentSQLiteDriver

// configures your application
public func configure(_ app: Application) async throws {
    // uncomment to serve files from /Public folder
    // app.middleware.use(FileMiddleware(publicDirectory: app.directory.publicDirectory))

    app.databases.use(.sqlite(.file("Persistence/db.sqlite")), as: .sqlite)
    app.databases.default(to: .sqlite)
    app.databases.use(.sqlite(.file("Persistence/lyrics.sqlite")), as: .lyrics)

    app.migrations.add(UserMigration(), to: .sqlite)
    app.migrations.add(AddProcessingStatusMigration(), to: .sqlite)
    app.migrations.add(UserSongCreationMigration(), to: .sqlite)
    app.migrations.add(SongMigration(), to: .sqlite)

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
