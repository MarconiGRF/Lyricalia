package br.dev.marconi.lyricalia.activities.match

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
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
import br.dev.marconi.lyricalia.activities.MatchPlayers
import br.dev.marconi.lyricalia.databinding.ActivityMatchWaitingBinding
import br.dev.marconi.lyricalia.enums.HostCommands
import br.dev.marconi.lyricalia.enums.PlayerMessages
import br.dev.marconi.lyricalia.repositories.match.PlayerInfo
import br.dev.marconi.lyricalia.repositories.match.toNonSer
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.match.MatchWaitingViewModel
import br.dev.marconi.lyricalia.viewModels.match.MatchWaitingViewModelFactory
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MatchWaitingActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchWaitingBinding
    private lateinit var viewModel: MatchWaitingViewModel

    private var players = mutableMapOf<String, Int>()
    private lateinit var otherPlayersLayout: LinearLayout
    private lateinit var playerColors: Array<Pair<Int, Int>>
    private var matchPlayers = MatchPlayers()

    override fun onStart() {
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = MatchWaitingViewModelFactory(applicationContext.filesDir, lifecycleScope)
        viewModel = ViewModelProvider(this, vmFactory)[MatchWaitingViewModel::class.java]
        viewModel.matchId = intent.extras!!.getString(NavigationUtils.MATCH_ID_PARAMETER_ID)!!
        viewModel.isHost = intent.extras!!.getBoolean(NavigationUtils.IS_HOST_PARAMETER_ID)

        setupCommonUI()
        if (viewModel.isHost) setupAsHost() else setupAsPlayer()
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
            if (it == false) { ceaseMatch() }
        }

        viewModel.actionable.observe(this) {
            if (it !== null) { processActionable(it) }
        }

        binding.matchId.text = viewModel.matchId
    }

    private fun processActionable(messageParts: List<String>) {
        when (messageParts[0]) {
            PlayerMessages.ENTITY -> processPlayerActionable(messageParts)
            HostCommands.ENTITY -> processHostActionable(messageParts)
            else -> { toastUnknownMessage(messageParts.joinToString("$")) }
        }
    }

    private fun processPlayerActionable(messageParts: List<String>) {
        when(messageParts[1]) {
            PlayerMessages.RECEIVABLE_JOINED -> {
                val playerInfo = Gson().fromJson(messageParts[2], PlayerInfo::class.java)
                addPlayerOnView(playerInfo)
            }
            PlayerMessages.RECEIVABLE_LEFT -> {
                removePlayer(messageParts[2])
            }
            else -> { toastUnknownMessage(messageParts.joinToString("$")) }
        }
    }

    private fun processHostActionable(messageParts: List<String>) {
        when(messageParts[1]) {
            HostCommands.RECEIVABLE_END -> { ceaseMatch() }
            HostCommands.RECEIVABLE_START -> { prepareMatch() }
            else -> { toastUnknownMessage(messageParts.joinToString("$")) }
        }
    }

    private fun setupAsHost() {
        viewModel.connectAsHost()

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

    private fun setupAsPlayer() {
        viewModel.connectAsPlayer()

        binding.closeButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sair?")
                .setMessage("Você será desconectado da partida")
                .setNeutralButton("CANCELAR") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("SAIR") { dialog, _ ->
                    dialog.dismiss()
                    viewModel.leaveMatch()
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

    private fun ceaseMatch() {
        AlertDialog.Builder(this)
            .setTitle("Partida encerrada pelo servidor")
            .setCancelable(false)
            .setNegativeButton("OK") { dialog, _ ->
                dialog.dismiss()
                NavigationUtils.navigateToMenu(this)
            }
            .create().show()
    }

    private fun prepareMatch() {
        binding.closeButton.visibility = INVISIBLE
        binding.mainContent.visibility = INVISIBLE
        lifecycleScope.launch { countdown() }
    }

    suspend fun countdown() {
        otherPlayersLayout.animate()
            .translationY(500f)
            .setDuration(550)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.countdownOutline.alpha = 0f
        binding.countdown.alpha = 0f
        binding.countdownOutline.visibility = VISIBLE
        binding.countdown.visibility = VISIBLE

        binding.countdownOutline.animate()
            .alpha(1f)
            .setDuration(750)
            .start()
        binding.countdown.animate()
            .alpha(1f)
            .setDuration(750)
            .start()

        var countdown = 3
        while (countdown >= 1) {
            MediaPlayer.create(applicationContext, R.raw.tick).start()
            binding.countdown.text = countdown.toString()

            delay(1000)
            countdown--
        }

        MediaPlayer.create(applicationContext, R.raw.chime).start()
        binding.countdown.text = "\uD83C\uDFC1"

        delay(1000)

        viewModel
        NavigationUtils.navigateToMatchOngoing(this, viewModel.matchId, viewModel.isHost, matchPlayers)
    }

    private fun addPlayerOnView(playerInfo: PlayerInfo) {
        val playerIndicatorInstance = LayoutInflater.from(this)
            .inflate(R.layout.player_indicator, otherPlayersLayout, false)

        playerIndicatorInstance.id = View.generateViewId()
        players.put(playerInfo.id, playerIndicatorInstance.id)

        playerIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer).setOnClickListener {
            Toast.makeText(this, playerInfo.name, Toast.LENGTH_SHORT).show()
        }
        playerIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer).setColorFilter(playerColors[viewModel.currentColor].second)
        playerIndicatorInstance.findViewById<TextView>(R.id.playerGlyph).also {
            it.setTextColor(playerColors[viewModel.currentColor].first)
            it.text = playerInfo.name[0].uppercase().toString()
        }

        matchPlayers.players.add(playerInfo.toNonSer())
        matchPlayers.colors.add(
            arrayListOf(playerColors[viewModel.currentColor].first, playerColors[viewModel.currentColor].second)
        )

        viewModel.currentColor = (viewModel.currentColor + 1) % playerColors.size

        playerIndicatorInstance.translationY = 2000f
        otherPlayersLayout.addView(playerIndicatorInstance)

        if (players.size % 2 != 0) {
            playerIndicatorInstance.animate()
                .translationY(120f)
                .setDuration(550)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            playerIndicatorInstance.animate()
                .translationY(0f)
                .setDuration(550)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun removePlayer(playerId: String) {
        val playerIndicatorId = players[playerId]

        if (playerIndicatorId !== null) {
            val playerIndicatorInstance = otherPlayersLayout
                .findViewById<ConstraintLayout>(playerIndicatorId)
            otherPlayersLayout.removeView(playerIndicatorInstance)

            players.remove(playerId)
        }
    }

    private fun toastUnknownMessage(message: String) {
        Toast.makeText(
            this,
            "Mensagem desconhecida: $message",
            Toast.LENGTH_LONG
        ).show()
    }
}