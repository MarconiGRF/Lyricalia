//
//  user.swift
//  api
//
//  Created by Marconi Filho on 06/05/25.
//

import Fluent
import Vapor

final class User: Model, Content, @unchecked Sendable {
    static let schema = "users"

    @ID(key: .id)
    var id: UUID?
    
    @Field(key: "name")
    var name: String
    
    @Field(key: "username")
    var username: String
    
    @Field(key: "spotifyToken")
    var spotifyToken: String?
    
    @Field(key: "spotifyUserId")
    var spotifyUserId: String?
    
    init() {}
    
    init(username: String, name: String) {
        self.username = username
        self.name = name
    }
}
