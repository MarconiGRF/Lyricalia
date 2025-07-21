enum MatchMessages: String {
    case WAITING = "match$waiting"
    case READY = "match$ready$"
    case PROCESSING = "match$processing"
    case NO_SONGS = "match$no_songs"
    case CHALLENGE = "match$challenge$"
    case ANSWER = "match$answer$"
    case CHALLENGE_END = "match$challenge$end"
    case COUNTDOWN = "match$countdown$"
    case PODIUM = "match$podium$"
    case FINAL_PODIUM = "match$final_podium$"
    case SUBMITTED = "match$submitted$"
}