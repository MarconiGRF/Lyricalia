
import Vapor

class UserLibraryProcessorQueue: @unchecked Sendable {
    @MainActor
    static let shared: UserLibraryProcessorQueue = { return UserLibraryProcessorQueue() }()

    private init () {}

    private var processingQueue: [UUID : LibraryProcessingStatus] = [:]

    func append(_ libStatus: LibraryProcessingStatus) {
        self.processingQueue[libStatus.userId] = libStatus
    }

    func getStatusForUserID(userId: UUID) -> LibraryProcessingStatus? {
        return self.processingQueue[userId]
    }

    func alreadyExists(userId: UUID) -> Bool {
        return self.processingQueue[userId] != nil
    }

    func getSize() -> Int {
        return self.processingQueue.count
    }
}

// // Storage key for the service
// struct UserLibraryProcessorQueueKey: StorageKey {
//     typealias Value = UserLibraryProcessorQueue
// }
