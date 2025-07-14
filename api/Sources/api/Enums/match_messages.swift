enum MatchMessages: String {
    case WAITING = "match$waiting"
    case READY = "match$ready$"
    case PROCESSING = "match$processing"
    case NO_SONGS = "match$no_songs"
    case CHALLENGE = "match$challenge$"
    case CHALLENGE_END = "match$challenge$end"
    case COUNTDOWN = "match$countdown$"
}