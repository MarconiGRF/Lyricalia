enum ExcerptSection: CaseIterable {
    case BEGGINING
    case MIDDLE
    case ENDING

    static func random() -> ExcerptSection {
        return allCases.randomElement()!
    }
}
