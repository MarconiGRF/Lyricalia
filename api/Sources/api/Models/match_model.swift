import struct Foundation.UUID
import Vapor
import Fluent
import SQLKit

class PlayingUser: @unchecked Sendable {
    let user: User

    var isReady: Bool
    var ws: WebSocket
    var score: Int64 = 0
    var submissions: [ [String] ] = []

    init(_ user: User, _ ws: WebSocket) {
        self.user = user
        self.ws = ws
        self.isReady = false
    }
}

class Match: @unchecked Sendable {
    let songLimit: Int
    let db: any Database
    let lyricsDB: any Database

    var hostId: UUID?
    var commonSongs: [Song] = []
    var chosenSongs: [Song] = []
    var lyrics: [String : Lyric] = [:]

    var totalTime = 5
    var countdown: Int = -1 {
        didSet {
            if (countdown >= 0) {
                for player in players {
                    Task {
                        try await player.ws.send(MatchMessages.COUNTDOWN.rawValue + "\(totalTime)/\(countdown)")
                    }
                }
                Task {
                    // It's a countdown, wait 1s, then decrease...
                    try await Task.sleep(nanoseconds: 1_000_000_000)
                    print("Counting down -1, currently at \(countdown)")
                    countdown -= 1
                }
            }
        }
    }

    // spotifySongId -> Verses
    var lyricsOriginalVerses: [String : [String]] = [:]
    var lyricalChallenges: [String : [String]] = [:]

    var currentChallengeIndex: Int? {
        didSet {
            if (currentChallengeIndex == nil) { return }

            if (currentChallengeIndex! <= chosenSongs.count - 1) {
                for player in players {
                    Task { do { try await player.ws.send(MatchMessages.CHALLENGE.rawValue + "\(currentChallengeIndex!)") } }
                }
            } else {
                for player in players {
                    Task { do { try await player.ws.send(MatchMessages.CHALLENGE_END.rawValue) } }
                }
            }
        }
    }
    var allReady: Bool {
        get { players.compactMap{ $0.isReady }.reduce(true) { $0 && $1 } }
    }
    var players: [PlayingUser] = [] {
        didSet { Task {
            var isHostIn = false
            players.compactMap { $0.user }.forEach {
                if ($0.id! == hostId) {
                    isHostIn = true
                    return
                }
            }

            if (!isHostIn) {
                for player in players {
                    Task { do { try await player.ws.send(HostCommands.RECEIVABLE_END.rawValue) } }
                }
            }
        }}
    }
    var inProgress: Bool = false {
        didSet { Task {
            print("Match progress changed! Notifying players")
            var removablePlayers: [UUID] = []

            for player in players {
                do { try await player.ws.send(
                        inProgress
                            ? HostCommands.SENDABLE_START.rawValue
                            : HostCommands.SENDABLE_END.rawValue
                    )
                }
                catch {
                    print("    !!! Could not communicate with player \(player.user.id!), assuming disconnected and removing.")
                    removablePlayers.append(player.user.id!)
                }
            }

            for removablePlayer in removablePlayers {
                do { await removePlayer(playerId: removablePlayer) }
            }
        }}
    }

    init(songLimit: Int, db: any Database, lyricsDB: any Database) {
        self.songLimit = songLimit
        self.db = db
        self.lyricsDB = lyricsDB
    }

    func setHost(hostId: String) throws {
        guard let uuid = UUID(uuidString: hostId) else {
            throw LyricaliaAPIError.inconsistency("Invalid UUID to be set as host of match")
        }
        self.hostId = uuid
    }

    func addPlayer(playerId: String, ws: WebSocket, db: any Database) async throws {
        guard let uuid = UUID(uuidString: playerId) else {
            throw LyricaliaAPIError.inconsistency("Invalid UUID to be set as host of match")
        }
        guard let user = try await User.find(uuid, on: db) else {
            throw LyricaliaAPIError.inconsistency("No user found for given UUID to be set as host of match")
        }

        let playingUser = PlayingUser(user, ws)

        for player in players {
            let jsonifiedPlayer = try JSONEncoder().encode(
                JoinedPlayer(id: uuid, name: user.name, username: user.username)
            )
            do {
                try await player.ws.send(
                    PlayerMessages.JOINED.rawValue + String(data: jsonifiedPlayer, encoding: .utf8)!
                )
            }
        }

        players.append(playingUser)
        for player in players {
            let jsonifiedPlayer = try JSONEncoder().encode(
                JoinedPlayer(id: player.user.id!, name: player.user.name, username: player.user.username)
            )
            do {
                try await ws.send(
                    PlayerMessages.JOINED.rawValue + String(data: jsonifiedPlayer, encoding: .utf8)!
                )
            }
        }
    }

    func start() {
        inProgress = true
    }

    func end() {
        inProgress = false
    }

    func removePlayer(playerId: String) async {
        let id = UUID(uuidString: playerId)
        if (id != nil) {
            await removePlayer(playerId: id!)
        }
    }

    func removePlayer(playerId: UUID) async {
        do {
            let playerIndex = players.firstIndex(where: { $0.user.id! == playerId })
            if playerIndex != nil {
                players.remove(at: playerIndex!)
            }

            for player in players {
                try await player.ws.send(PlayerMessages.LEFT.rawValue + playerId.uuidString)
            }

        } catch {
            print("    !!! Failed to remove player \(playerId.uuidString) due to \(error)")
        }
    }

    func ackReadiness(playerId: String, ws: WebSocket) async {
        do {
            let player = players.first { $0.user.id!.uuidString == playerId }
            if (player == nil) { throw LyricaliaAPIError.inconsistency("No player found to be ready with id \(playerId)") }
            player!.isReady = true

            try await player!.ws.close(code: .normalClosure)
            player!.ws = ws

            if (allReady) {
                print("    -> All players ready!")

                for player in players {
                    Task { try await player.ws.send(MatchMessages.PROCESSING.rawValue) }
                    player.isReady = false
                }

                Task { try await process() }
            } else {
                try await player!.ws.send(MatchMessages.WAITING.rawValue)
            }
        } catch {
            print("    !!! Failed to ack player readinesss \(error)")
        }
    }

    func process() async throws {
        print("Processing match...")

        do {
            try await findCommonSongsOrEndMatch()
                      getRandomSongs()
                await fetchLyrics()
                      createLyricalChallenges()
            try await sendChallenges()
        } catch {
            print("    !!! Error processing match's songs -> \(error)")
        }
    }

    func ackChallenge(playerId: String) async {
        do {
            let player = players.first { $0.user.id!.uuidString == playerId }
            if (player == nil) { throw LyricaliaAPIError.inconsistency("No player found to be ready for challenge with id \(playerId)") }
            player!.isReady = true

            if (allReady) {
                print("    -> All players ready to challenge!")

                if (currentChallengeIndex == nil) {
                    currentChallengeIndex = 0
                } else {
                    currentChallengeIndex! += 1
                }

                players.forEach { $0.isReady = false }
            }
        } catch {
            print("    !!! Failed to ack player readinesss for challenge -> \(error)")
        }
    }

    func ackInput(playerId: String) async {
        do {
            let player = players.first { $0.user.id!.uuidString == playerId }
            if (player == nil) { throw LyricaliaAPIError.inconsistency("No player found to be ready for input with id \(playerId)") }
            player!.isReady = true

            if (allReady) {
                print("    -> All players ready to input!")

                self.countdown = self.totalTime

                players.forEach { $0.isReady = false }
            }
        } catch {
            print("    !!! Failed to ack player readinesss for input -> \(error)")
        }
    }

    func ackSubmission(playerId: String, submission: String) async {
        do {
            let player = players.first { $0.user.id!.uuidString == playerId }
            if (player == nil) { throw LyricaliaAPIError.inconsistency("No player found to receive submission with id \(playerId)") }
            player!.isReady = true
            player!.submissions.append(submission.components(separatedBy: "\n"))

            if (allReady) {
                print("    -> All players submitted their answers!")

                self.countdown = 0
                calculateScores()
                sendPodium()

                players.forEach { $0.isReady = false }
            }
        } catch {
            print("    !!! Failed to ack player submission -> \(error)")
        }
    }








    private func sendPodium() {
        // Condense and send podium
    }

    private func calculateScores() {
        do {
            var finalScore = 0
            let totalSizeSimilarityPoints = 1000
            let totalContentSimilarityPoints = 4000

            // on the current challenge index, do:
            let lyricsSection = lyricalChallenges[chosenSongs[currentChallengeIndex!].spotifyId]!
            for (verseIndex, verse) in lyricsSection.enumerated() {
                if !verse.starts(with: "lyChal_") { continue }

                let originalVerse = lyricsOriginalVerses[
                    chosenSongs[
                        currentChallengeIndex!
                    ].spotifyId
                ]![verseIndex]

                for player in players {
                    let playerSectionSubmission = player.submissions[currentChallengeIndex!]

                    // Calculate points by size similarity
                    // The farthest the player verse submission size is from the original size, the more points player loses
                    // This will return a 0-1 percentage, thus * 1000 is applied
                    let playerVerseSize = Double(playerSectionSubmission[verseIndex].count)
                    let originalVerseSize = Double(originalVerse.count)

                    let sizeDiscountPoints = (playerVerseSize / originalVerseSize) * 1000
                    finalScore += totalSizeSimilarityPoints - Int(sizeDiscountPoints)

                    // Calculate points by content similarity
                    // The farthest the distance to make the strings equal, the more points player loses
                    /// This will return 0-N number, thus only * 100 is applied (also due to higher weight on final points)
                    let contentSimilarityDiscountPoints = LevenshteinDistance.calculate(
                        originalVerse, playerSectionSubmission[verseIndex]
                    ) * 100
                    finalScore += totalContentSimilarityPoints - contentSimilarityDiscountPoints
                }
            }
        }
    }

    private func sendChallenges() async throws {
        for player in players {
            do {
                let challengeSet = MatchChallengeSet(self.chosenSongs, self.lyricalChallenges)

                let encoder = JSONEncoder()
                encoder.outputFormatting = .withoutEscapingSlashes
                let jsonifiedChallenge = String(data: try encoder.encode(challengeSet), encoding: .utf8)!

                try await player.ws.send(MatchMessages.READY.rawValue + jsonifiedChallenge)
            } catch { print("    !!! Couldn't send challenge to player \(player.user.id!) due to -> \(error)") }
        }
    }

    private func createLyricalChallenges() {
        for (spotifyId, lyric) in self.lyrics {
            // Guaranteeing that no 1-verse excerpt is selected.
            var backoffLimit = 0
            var excerptVerses = [""]
            while (excerptVerses.count == 1 && backoffLimit <= 5) {
                excerptVerses = lyric.plainLyrics
                    .components(separatedBy: "\n\n").randomElement()!.components(separatedBy: "\n")
                backoffLimit += 1
            }
            if (backoffLimit == 6 || excerptVerses.count <= 1) { continue }

            let deletionAmount = Int(ceil(Double(excerptVerses.count) / Double(3)))
            self.lyricsOriginalVerses[spotifyId] = excerptVerses

            if (excerptVerses.count == 2) {
                let deletableVerseIndex = (0..<1).randomElement()!
                let deletableVerse = excerptVerses[deletableVerseIndex]

                excerptVerses[deletableVerseIndex] = "lyChal_\(deletableVerse.count)"
                lyricalChallenges[spotifyId] = excerptVerses

                continue
            }

            // Delete verses!
            switch ExcerptSection.random() {
                case ExcerptSection.BEGGINING:
                    var idx = 0
                    while (idx < deletionAmount) {
                        excerptVerses[idx] = "lyChal_\(excerptVerses[idx].count)"
                        idx += 1
                    }
                    lyricalChallenges[spotifyId] = excerptVerses

                case ExcerptSection.MIDDLE:
                    var idx = deletionAmount - 1
                    while (idx < (deletionAmount - 1) + deletionAmount && idx < excerptVerses.count) {
                        excerptVerses[idx] = "lyChal_\(excerptVerses[idx].count)"
                        idx += 1
                    }
                    lyricalChallenges[spotifyId] = excerptVerses

                case ExcerptSection.ENDING:
                    var idx = excerptVerses.count - 1
                    while ( (idx > excerptVerses.count - deletionAmount) && idx >= 0 ) {
                        excerptVerses[idx] = "lyChal_\(excerptVerses[idx].count)"
                        idx -= 1
                    }
                    lyricalChallenges[spotifyId] = excerptVerses
            }
        }
    }

    private func fetchLyrics() async  {
        do {
            for song in chosenSongs {
                let normalizedSongName = normalizeSongInfo(songInfo: song.name)
                let normalizedArtistName = normalizeSongInfo(songInfo: song.artist)

                let lyric = try await (self.lyricsDB as! any SQLDatabase).raw("""
                    SELECT plain_lyrics, synced_lyrics, track_id FROM lyrics
                    WHERE id IN (
                        SELECT last_lyrics_id FROM tracks
                        WHERE
                            artist_name_lower = '\(normalizedArtistName)' AND name_lower = '\(normalizedSongName)'
                        LIMIT 1
                    )
                    LIMIT 1
                    """).all(decodingFluent: Lyric.self)

                if lyric.count > 0 { self.lyrics[song.spotifyId] = lyric[0] }
                else {
                    print("    -> Lyric not found for song '\(normalizedArtistName)' - '\(normalizedSongName)'")
                }
            }
        } catch {
            print("    !!! Failed to fetch lyrics -> \(error)")
        }
    }

    private func normalizeSongInfo(songInfo: String) -> String {
        let diacriticInsensitiveLowercaseInfo = songInfo.folding(options: .diacriticInsensitive, locale: .current).lowercased()

        let specialCharactersRegex = #/[`~!@#$%^&*()_|+\-=?;:",.<>\{\}\[\]\\\/]/#
        let quotesRegex = #/[\'’]/#
        let whitespaceCollapseRegex = #/\s{2,}/#

        return diacriticInsensitiveLowercaseInfo
            .replacing(specialCharactersRegex, with: " ")
            .replacing(quotesRegex, with: "")
            .replacing(whitespaceCollapseRegex, with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func getRandomSongs() {
        var chosenSongs: [Song] = []

        var remainingSongs = songLimit
        while (remainingSongs > 0 && self.commonSongs.count > 0) {
            chosenSongs.append(self.commonSongs.remove(at: commonSongs.indices.randomElement()!))
            remainingSongs -= 1
        }

        self.chosenSongs = chosenSongs
    }

    private func findCommonSongsOrEndMatch() async throws {
        do {
            let userIds = self.players.compactMap{ $0.user.id }

            commonSongs = try await (self.db as! any SQLDatabase).raw("""
                SELECT * FROM songs
                WHERE id IN (
                    SELECT song_id FROM 'users+songs'
                    WHERE user_id IN (\( self.players.compactMap{ "'\($0.user.id!.uuidString)'" }.joined(separator: ",") ))
                    GROUP BY song_id HAVING COUNT(DISTINCT user_id) = \(bind: userIds.count)
                )
                """)
                .all(decodingFluent: Song.self)

            if (commonSongs.count == 0) {
                for player in players {
                    try await player.ws.send(MatchMessages.NO_SONGS.rawValue)
                }
                throw LyricaliaAPIError.notFound("No common songs found for the current player set")
            }
        } catch {
            print("    !!! Failed to find common songs between users due to -> \(error)")
            throw error
        }
    }
}
