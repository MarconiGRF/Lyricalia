package br.dev.marconi.lyricalia.activities.match

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.databinding.ActivityMatchWaitingBinding
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.MatchWaitingViewModel
import br.dev.marconi.lyricalia.viewModels.MatchWaitingViewModelFactory

class MatchWaitingActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchWaitingBinding
    private lateinit var viewModel: MatchWaitingViewModel

    private var players = mutableMapOf<String, Int>()
    private lateinit var otherPlayersLayout: LinearLayout
    private lateinit var playerColors: Array<Pair<Int, Int>>

    override fun onStart() {
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = MatchWaitingViewModelFactory(applicationContext.filesDir, lifecycleScope)
        viewModel = ViewModelProvider(this, vmFactory)[MatchWaitingViewModel::class.java]

        setupCommonUI()

        val matchId = intent.extras!!.getString(NavigationUtils.MATCH_ID_PARAMETER_ID)!!
        binding.matchId.text = matchId

        val isHost = intent.extras!!.getBoolean(NavigationUtils.IS_HOST_PARAMETER_ID)
        if (isHost) setupAsHost(matchId) else setupAsPlayer(matchId)
    }

    private fun setupCommonUI() {
        binding = ActivityMatchWaitingBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        // TODO: Fix white icons on dark mode UI with absolutely zero contrast

        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        playerColors = arrayOf(
            Pair(resources.getColor(R.color.lyWhite, theme), resources.getColor(R.color.lyIndigo, theme)),
            Pair(resources.getColor(R.color.lyWhite, theme), resources.getColor(R.color.lyDarkerGray, theme)),
            Pair(resources.getColor(R.color.lyDarkerGray, theme), resources.getColor(R.color.lyGreen, theme)),
            Pair(resources.getColor(R.color.lyGreen, theme), resources.getColor(R.color.lyGray, theme)),
            Pair(resources.getColor(R.color.lyIndigo, theme), resources.getColor(R.color.lyDarkerGray, theme))
        )

        otherPlayersLayout = binding.otherPlayers

        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.APPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.CHANGE_APPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING)
        layoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
        layoutTransition.setDuration(LayoutTransition.APPEARING, 350)
        layoutTransition.setDuration(LayoutTransition.CHANGE_APPEARING, 350)
        otherPlayersLayout.layoutTransition = layoutTransition

        viewModel.hostOnline.observe(this) {
            if (it == false) {
                AlertDialog.Builder(this)
                    .setTitle("Partida encerrada pelo servidor")
                    .setCancelable(false)
                    .setNegativeButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        NavigationUtils.navigateToMenu(this)
                    }
                    .create().show()
            }
        }
    }

    private fun setupAsHost(matchId: String) {
        viewModel.connectAsHost(matchId)

        binding.closeButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sair?")
                .setMessage("Isso desconectará os outros jogadores")
                .setNegativeButton("SAIR") { dialog, _ ->
                    dialog.dismiss()
                    viewModel.endMatch()
                    NavigationUtils.navigateToMenu(this)
                }
                .create().show()
        }

        binding.actionMatchButton.setOnClickListener {
            viewModel.startMatch()
        }
    }

    private fun setupAsPlayer(matchId: String) {
        viewModel.connectAsPlayer(matchId)

        binding.closeButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sair?")
                .setMessage("Você será desconectado da partida")
                .setNeutralButton("CANCELAR") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("SAIR") { dialog, _ ->
                    dialog.dismiss()
                    NavigationUtils.navigateToMenu(this)
                }
                .create().show()
        }

        binding.actionMatchButton.isClickable = false
        binding.actionMatchButton.text = "Aguardando host..."
        binding.actionMatchButton.setTextColor(
            resources.getColor(R.color.lyWhite, theme)
        )
        binding.actionMatchButton.setBackgroundColor(resources.getColor(R.color.lyDarkerGray, theme))
    }

    private fun addPlayerOnView() {
        val playerIndicatorInstance = LayoutInflater.from(this)
            .inflate(R.layout.player_indicator, otherPlayersLayout, false)

        val playerName = ('A'..'Z').random().toString()
        playerIndicatorInstance.id = View.generateViewId()
        players.put(playerName, playerIndicatorInstance.id)

        playerIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer)
            .setColorFilter(playerColors[viewModel.getCurrentColor()].second)
        playerIndicatorInstance.findViewById<TextView>(R.id.playerGlyph).also {
            it.setTextColor(playerColors[viewModel.getCurrentColor()].first)
            it.text = playerName
        }
        viewModel.incrementCurrentColor(playerColors.size)

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
}