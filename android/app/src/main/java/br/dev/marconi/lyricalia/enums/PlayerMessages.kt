package br.dev.marconi.lyricalia.enums

class PlayerMessages {
    companion object {
        // SENDABLES
        fun JOIN(userId: String) = "player\$join$$userId"

        // RECEIVABLES
        fun JOINED(userId: String) = "player\$joined$$userId"
        fun LEFT(userId: String) = "player\$left$$userId"
    }
}