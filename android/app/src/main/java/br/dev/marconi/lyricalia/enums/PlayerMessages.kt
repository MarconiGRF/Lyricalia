package br.dev.marconi.lyricalia.enums

class PlayerMessages {
    companion object {
        const val ENTITY = "player"

        const val RECEIVABLE_JOINED = "joined"
        const val RECEIVABLE_LEFT = "left"

        // SENDABLES
        fun JOIN(userId: String) = "player\$join$$userId"
        fun LEAVE(userId: String) = "player\$leave$$userId"
        fun READY(userId: String) = "player\$ready$$userId"
        fun CHALLENGE_READY(userId: String) = "player\$challenge_ready$$userId"
        fun INPUT_READY(userId: String) = "player\$input_ready$$userId"

        // RECEIVABLES
        fun JOINED(userId: String) = "player\$joined$$userId"
        fun LEFT(userId: String) = "player\$left$$userId"
    }
}
