struct LevenshteinDistance {
    static func calculate(_ sequence1: String, _ sequence2: String) -> Int {
        let empty = [Int](repeating:0, count: sequence2.count)
        var last = [Int](0...sequence2.count)

        for (i, char1) in sequence1.enumerated() {
            var cur = [i + 1] + empty
            for (j, char2) in sequence2.enumerated() {
                cur[j + 1] = char1 == char2 ? last[j] : min(last[j], last[j + 1], cur[j]) + 1
            }
            last = cur
        }
        return last.last!
    }
}
