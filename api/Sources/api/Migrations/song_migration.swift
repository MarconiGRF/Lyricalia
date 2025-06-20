//
//  user_migration.swift
//  api
//
//  Created by Marconi Filho on 06/05/25.
//

import Fluent

struct SongMigration: AsyncMigration {
    func revert(on database: any FluentKit.Database) async throws {
        try await database.schema("songs").delete()
    }

    func prepare(on database: any Database) async throws {
        try await database.schema("songs")
            .id()
            .field("name", .string, .required)
            .field("artist", .string, .required)
            .field("spotifyId", .string, .required)
            .create()
    }
}
