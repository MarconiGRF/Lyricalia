enum PlayerMessages: String {
    // SENDABLES
    case JOIN = "join"
    case LEAVE = "leave"
    case READY = "ready"

    // RECEIVABLES
    case JOINED = "player$joined$"
    case LEFT = "player$left$"
}