import Foundation

class UserLibraryProcessor: Thread, @unchecked Sendable {
    let waiter = DispatchGroup()
    var libStatus: LibraryProcessingStatus

    override func start() {
        waiter.enter()
        super.start()
    }

    override func main() {
        Task {
            while(libStatus.processedItems < libStatus.totalItems) {
                try await Task.sleep(nanoseconds: 3000000000)

                print("\(libStatus.userId) - adding to processed items - \(libStatus.processedItems) of \(libStatus.totalItems)")
                libStatus.processedItems += 10
            }
        }
    }

    init(libStatus: LibraryProcessingStatus) {
        self.libStatus = libStatus
    }
}