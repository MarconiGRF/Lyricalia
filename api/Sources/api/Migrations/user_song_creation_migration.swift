import Fluent

struct UserSongCreationMigration: AsyncMigration {
    func prepare(on database: any Database) async throws {
        try await database.schema("users+songs")
            .id()
            .field("user_id", .uuid, .required, .references("users", "id", onDelete: .cascade))
            .field("song_id", .uuid, .required, .references("songs", "id", onDelete: .cascade))
            .unique(on: "song_id", "user_id")
            .create()
    }

    func revert(on database: any Database) async throws {
        try await database.schema("users+songs").delete()
    }
}