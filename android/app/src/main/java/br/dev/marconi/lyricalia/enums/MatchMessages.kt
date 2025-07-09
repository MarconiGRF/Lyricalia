package br.dev.marconi.lyricalia.enums

class MatchMessages {
    companion object {
        const val ENTITY = "match"

        const val RECEIVABLE_WAITING = "waiting"
        const val RECEIVABLE_PROCESSING = "processing"
        const val RECEIVABLE_PLAYERS = "players"
        const val RECEIVABLE_READY = "ready"
        const val RECEIVABLE_READY_FULL = "$ENTITY\$ready"
        const val RECEIVABLE_PROGRESS = "progress"
        const val RECEIVABLE_PODIUM = "podium"

        // SENDABLES
        fun PROGRESS(userId: String) = "player\$progress$$userId"
        fun SUBMISSION(userId: String, submission: String) = "player\$progress$$userId$$submission"
        fun LEAVE(userId: String) = "player\$leave$$userId"
        fun READY(userId: String) = "player\$ready$$userId"
    }
}