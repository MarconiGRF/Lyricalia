import Vapor

struct CreateMatchRequest: Content {
    let songLimit: Int
}