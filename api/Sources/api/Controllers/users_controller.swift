//
//  users.swift
//  api
//
//  Created by Marconi Filho on 06/05/25.
//

import Vapor
import Fluent

struct UsersController: RouteCollection {
    func boot(routes: any RoutesBuilder) throws {
        let users = routes.grouped("users")
        users.get(use: getAllUsers)
        users.post(use: create)
        
        users.group(":username") { user in
            user.get(use: getUser)
        }
    }
    
    func getAllUsers(req: Request) async throws -> [String] {
        let users = try await User.query(on: req.db).all()
        return users.map { $0.username }
    }
    
    func getUser(req: Request) async throws -> User {
        let username = req.parameters.get("username")!
        let user = try await findUser(req.db, username)
        
        return try user ?? { throw Abort(.notFound) }()
    }

    func create(req: Request) async throws -> User {
        let user = try req.content.decode(User.self)
        
        let existingUser = try await findUser(req.db, user.username)
        if existingUser != nil { return existingUser! }
        
        try await user.save(on: req.db)
        
        return user
    }
    
    private func findUser(_ db: any Database, _ username: String) async throws -> User? {
        try await User.query(on: db).filter(\.$username == username).first()
    }

}
