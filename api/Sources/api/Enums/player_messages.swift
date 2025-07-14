enum PlayerMessages: String {
    // SENDABLES
    case JOIN = "join"
    case LEAVE = "leave"
    case READY = "ready"
    case CHALLENGE_READY = "challenge_ready"
    case INPUT_READY = "input_ready"

    // RECEIVABLES
    case JOINED = "player$joined$"
    case LEFT = "player$left$"
}