enum LyricaliaAPIError: Error {
    case inconsistency(String)
    case notFound(String)
    case invalidCommand
}