import Foundation
import Fluent

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
                try await Task.sleep(nanoseconds: 500000000)

                libStatus.processedItems += 1

                if (libStatus.webSocket != nil) {
                    try await libStatus.webSocket!.send("\(libStatus.processedItems)")
                }
            }

            let user = try await User.find(libStatus.userId, on: libStatus.db)
            guard user != nil else {
                if (libStatus.webSocket != nil) { try await libStatus.webSocket!.close(code: .unexpectedServerError) }
                throw LyricaliaAPIError.inconsistency("NO USER FOR PROCESSED LIBRARY, I WILL DO ABSOLUTELY NOTHING ABOUT THIS!!!")
            }
            user!.isLibraryProcessed = true
            try await user!.save(on: libStatus.db)

            if (libStatus.webSocket != nil) {
                try await libStatus.webSocket!.send("Done!")
                try await libStatus.webSocket!.close(code: .normalClosure)
                print("Done processing library \(libStatus.processedItems)")
            }
        }
    }

    init(_ libStatus: LibraryProcessingStatus) {
        self.libStatus = libStatus
    }
}