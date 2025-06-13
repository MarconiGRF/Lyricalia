//
//  user_migration.swift
//  api
//
//  Created by Marconi Filho on 06/05/25.
//

import Fluent

struct AddProcessingStatusMigration: AsyncMigration {
    func revert(on database: any FluentKit.Database) async throws {
        try await database.schema("users").delete()
    }

    func prepare(on database: any Database) async throws {
        try await database.schema("users")
            .field("isLibraryProcessed", .bool)
            .update()
    }
}
