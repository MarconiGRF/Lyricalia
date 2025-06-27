package br.dev.marconi.lyricalia.activities.match

import android.animation.LayoutTransition
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.databinding.ActivityMatchWaitingBinding
import br.dev.marconi.lyricalia.utils.NavigationUtils

class MatchWaitingActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchWaitingBinding
    private lateinit var otherPlayersLayout: LinearLayout
    private lateinit var playerColors: Array<Pair<Int, Int>>
    private var currentColor = 0
    private var players = mutableMapOf<String, Int>()

    override fun onStart() {
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMatchWaitingBinding.inflate(layoutInflater)
        setupMatchWaitingActivity()

        playerColors = arrayOf(
            Pair(resources.getColor(R.color.lyWhite, theme), resources.getColor(R.color.lyIndigo, theme)),
            Pair(resources.getColor(R.color.lyWhite, theme), resources.getColor(R.color.lyDarkerGray, theme)),
            Pair(resources.getColor(R.color.lyDarkerGray, theme), resources.getColor(R.color.lyGreen, theme)),
            Pair(resources.getColor(R.color.lyGreen, theme), resources.getColor(R.color.lyGray, theme)),
            Pair(resources.getColor(R.color.lyIndigo, theme), resources.getColor(R.color.lyDarkerGray, theme))
        )

        otherPlayersLayout = binding.otherPlayers
        binding.createMatchButton.setOnClickListener { addPlayerOnView() }

        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.APPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.CHANGE_APPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
        layoutTransition.setDuration(LayoutTransition.APPEARING, 350)
        layoutTransition.setDuration(LayoutTransition.CHANGE_APPEARING, 350)
        otherPlayersLayout.layoutTransition = layoutTransition

        val matchId = intent.extras!!.getString(NavigationUtils.MATCH_ID_PARAMETER_ID)!!
        binding.matchId.text = matchId

        val isHost = intent.extras!!.getBoolean(NavigationUtils.IS_HOST_PARAMETER_ID)
    }

    private fun addPlayerOnView() {
        val playerIndicatorInstance = LayoutInflater.from(this)
            .inflate(R.layout.player_indicator, otherPlayersLayout, false)

        val playerName = ('A'..'Z').random().toString()
        playerIndicatorInstance.id = View.generateViewId()
        players.put(playerName, playerIndicatorInstance.id)

        playerIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer)
            .setColorFilter(playerColors[currentColor].second)
        playerIndicatorInstance.findViewById<TextView>(R.id.playerGlyph).also {
            it.setTextColor(playerColors[currentColor].first)
            it.text = playerName
        }
        currentColor = (currentColor + 1) % playerColors.size

        playerIndicatorInstance.translationY = 2000f
        otherPlayersLayout.addView(playerIndicatorInstance)

        if (players.size % 2 != 0) {
            playerIndicatorInstance.animate()
                .translationY(120f)
                .setDuration(550)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            playerIndicatorInstance.animate()
                .translationY(0f)
                .setDuration(550)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
    private fun removePlayer(playerIndicatorId: Int) {
        val playersContainer = binding.otherPlayers

        val playerIndicatorInstance = playersContainer.findViewById<ConstraintLayout>(playerIndicatorId)
        playersContainer.removeView(playerIndicatorInstance)
    }

    private fun setupMatchWaitingActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)

        // TODO: Fix white icons on dark mode UI with absolutely zero contrast

        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}