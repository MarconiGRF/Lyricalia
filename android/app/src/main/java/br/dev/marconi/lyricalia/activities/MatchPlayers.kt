package br.dev.marconi.lyricalia.activities

import android.os.Parcelable
import br.dev.marconi.lyricalia.repositories.match.NonSerPlayerInfo

import kotlinx.parcelize.Parcelize

@Parcelize
class MatchPlayers(
    var players: ArrayList<NonSerPlayerInfo> = arrayListOf(),
    var colors: ArrayList<ArrayList<Int>> = arrayListOf(),
    var viewsId: ArrayList<Int> = arrayListOf()
) : Parcelable
