enum HostCommands: String {
    case RECEIVABLE_SET = "set"
    case RECEIVABLE_START = "start"
    case RECEIVABLE_END = "end"

    case SENDABLE_START = "host$start"
    case SENDABLE_END = "host$end"
}

/*
Host -> Join
[Host]

notify: Host
-> joined: Host

Player -> Join
[Host, Player]


*/