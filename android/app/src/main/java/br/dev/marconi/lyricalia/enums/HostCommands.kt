package br.dev.marconi.lyricalia.enums

class HostCommands {
    companion object {
        // SENDABLES
        fun SET(userId: String) = "host\$set$${userId}"
        const val START = "host\$start"

        // RECEIVABLES
        const val END = "host\$end"
    }
}