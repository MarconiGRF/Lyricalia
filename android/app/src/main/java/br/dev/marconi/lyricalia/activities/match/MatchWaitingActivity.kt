package br.dev.marconi.lyricalia.activities.match

import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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

    override fun onStart() {
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMatchWaitingBinding.inflate(layoutInflater)
        setupMatchWaitingActivity()

        binding.createMatchButton.setOnClickListener {
            addPlayerOnView()
        }
        binding.deleteppl.setOnClickListener {
            removePlayer(players.values.random())
        }

        val matchId = intent.extras!!.getString(NavigationUtils.MATCH_ID_PARAMETER_ID)!!
        binding.matchId.text = matchId

        val isHost = intent.extras!!.getBoolean(NavigationUtils.IS_HOST_PARAMETER_ID)
    }

    private var players = mutableMapOf<String, Int>()
    private fun addPlayerOnView() {
        val playersContainer = binding.otherPlayers

        val playerIndicatorInstance = LayoutInflater.from(this)
            .inflate(R.layout.player_indicator, playersContainer, false)

        val blueAmt = IntArray(256) { it + 1 }
        playerIndicatorInstance.id = View.generateViewId()

        if (players.size % 2 != 0) {
            playerIndicatorInstance.translationY = 120f
        }
        val playerName = ('A'..'Z').random().toString()
        players.put(playerName, playerIndicatorInstance.id)

        playerIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer)
            .setColorFilter(Color.argb(255, 255, 255, blueAmt.random()))

        playerIndicatorInstance.findViewById<TextView>(R.id.playerGlyph).also {
            it.setTextColor(Color.argb(255, blueAmt.random(), 255, 255))
            it.text = playerName
        }

        playersContainer.addView(playerIndicatorInstance)
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