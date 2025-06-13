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
                try await Task.sleep(nanoseconds: 50000000)


                print("\(libStatus.userId) - adding to processed items - \(libStatus.processedItems) of \(libStatus.totalItems)")
                libStatus.processedItems += 1

                if (libStatus.webSocket != nil) {
                    try await libStatus.webSocket!.send("\(libStatus.processedItems)")
                    print("Sending info through websocket")
                } else {
                    print("No websocket for user library \(libStatus.userId)")
                }
            }

            if (libStatus.webSocket != nil) {
                // try await libStatus.webSocket!.send("Done!")
                try await libStatus.webSocket!.close(code: .normalClosure)
                print("Done processing library \(libStatus.processedItems)")
            }

            
        }
    }

    init(libStatus: LibraryProcessingStatus) {
        self.libStatus = libStatus
    }
}