enum PlayerMessages: String {
    // SENDABLES
    case JOIN = "join"

    // RECEIVABLES
    case JOINED = "player$joined$"
    case LEFT = "player$left$"
}