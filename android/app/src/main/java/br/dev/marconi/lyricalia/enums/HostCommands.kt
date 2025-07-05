package br.dev.marconi.lyricalia.enums

class HostCommands {
    companion object {
        const val ENTITY = "host"
        const val RECEIVABLE_END = "end"
        const val RECEIVABLE_START = "start"

        // SENDABLES
        fun SET(userId: String) = "host\$set$${userId}"
        const val START = "host\$start"

        // RECEIVABLES
        const val SENDABLE_END = "host\$end"
    }
}