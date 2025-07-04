enum PlayerMessages: String {
    // SENDABLES
    case JOIN = "join"
    case LEAVE = "leave"

    // RECEIVABLES
    case JOINED = "player$joined$"
    case LEFT = "player$left$"
}