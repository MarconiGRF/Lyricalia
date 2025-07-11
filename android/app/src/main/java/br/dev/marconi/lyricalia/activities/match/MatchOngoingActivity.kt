package br.dev.marconi.lyricalia.activities.match

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import br.dev.marconi.lyricalia.databinding.ActivityMatchOngoingBinding
import br.dev.marconi.lyricalia.enums.HostCommands
import br.dev.marconi.lyricalia.enums.MatchMessages
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.match.MatchOngoingViewModel
import br.dev.marconi.lyricalia.viewModels.match.MatchOngoingViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Int

class MatchOngoingActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchOngoingBinding
    private lateinit var viewModel: MatchOngoingViewModel

    private var isGclefAnimated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = MatchOngoingViewModelFactory(applicationContext.filesDir, lifecycleScope)
        viewModel = ViewModelProvider(this, vmFactory)[MatchOngoingViewModel::class.java]
        viewModel.matchId = intent.extras!!.getString(NavigationUtils.MATCH_ID_PARAMETER_ID)!!
        viewModel.isHost = intent.extras!!.getBoolean(NavigationUtils.IS_HOST_PARAMETER_ID)

        setupCommonUI()

        viewModel.connectToMatch()
        showLoadingOverlays(true)
    }

    private fun processActionable(messageParts: List<String>) {
        when (messageParts[0]) {
            MatchMessages.ENTITY -> processMatchActionable(messageParts)
            HostCommands.ENTITY -> processHostActionable(messageParts)
            else -> { toastUnknownMessage("0 " + messageParts.joinToString("$")) }
        }
    }

    private fun processHostActionable(messageParts: List<String>) {
        when (messageParts[1]) {
            HostCommands.RECEIVABLE_END -> ceaseMatch()
        }
    }

    private fun processMatchActionable(messageParts: List<String>) {
        when (messageParts[1]) {
            MatchMessages.RECEIVABLE_WAITING -> updateLoadingHint("Esperando outros jogadores...")
            MatchMessages.RECEIVABLE_PROCESSING -> updateLoadingHint("Pensando nas letras...")
            MatchMessages.RECEIVABLE_CHALLENGE -> processChallengeActionable(messageParts)
            MatchMessages.RECEIVABLE_READY -> {
                updateLoadingHint("Vamos lá!")
                lifecycleScope.launch {
                    delay(1000);
                    gracefullyHideLoading()
                    delay(1000);
                    viewModel.notifyReadinessToChallenge()
                }
            }
            else -> { toastUnknownMessage("1 " + messageParts.joinToString("$")) }
        }
    }

    private fun processChallengeActionable(messageParts: List<String>) {
        val challengeInfo = messageParts[2].toIntOrNull()
        when (challengeInfo) {
            null ->  toastUnknownMessage("null challenge actionable")
            in 0 .. 100 -> showChallengeHint(challengeInfo)
            else -> toastUnknownMessage("2 " + messageParts.joinToString("$"))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showChallengeHint(challengeIndex: Int) {
        binding.songNameHint.text = viewModel.challengeSet!!.songs[challengeIndex].name
        binding.artistHint.text = viewModel.challengeSet!!.songs[challengeIndex].artist

        binding.challengeIndex.text = "${challengeIndex + 1}/${viewModel.challengeSet!!.songs.size}"
        binding.challengeIndex.visibility = VISIBLE

        binding.currentChallengeHint.alpha = 0f
        binding.currentChallengeHint.visibility = VISIBLE
        binding.currentChallengeHint.animate()
            .alpha(1f)
            .setDuration(550)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        lifecycleScope.launch { setupChallenge(challengeIndex) }
                    }
                }
            )
            .start()
    }

    private suspend fun setupChallenge(challengeIndex: Int) {
        //TODO: Check if this is really necessary or section are too slow between them due to animations
        delay(3500)

        fadeHints()

        // build text fields

        // Set constraints of hint to release bottom constraint (AND DO IT ANIMATED)

        // Setup player indicators on the bottom
    }

    private fun fadeHints() {
        binding.challengeHint.animate()
            .alpha(0f)
            .setDuration(350)
            .start()

        TransitionManager.beginDelayedTransition(binding.mainLayout, AutoTransition().apply {
            duration = 550
            interpolator = DecelerateInterpolator()
        })

        ConstraintSet().also {
            it.clone(binding.currentChallengeHint)

            it.constrainHeight(binding.challengeHint.id, 0)
            it.clear(binding.songNameHint.id, ConstraintSet.BOTTOM)
            it.constrainHeight(binding.songNameHint.id, 28.fromDpToPx())
            it.constrainHeight(binding.artistHint.id, 18.fromDpToPx())

            it.applyTo(binding.currentChallengeHint)
        }
        ConstraintSet().also {
            it.clone(binding.mainLayout)
            it.clear(binding.currentChallengeHint.id, ConstraintSet.BOTTOM)
            it.applyTo(binding.mainLayout)
        }
    }

    private fun setupCommonUI() {
        binding = ActivityMatchOngoingBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (viewModel.isHost) {
            binding.closeButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Encerrar partida?")
                    .setMessage("Isso desconectará os outros jogadores")
                    .setNegativeButton("ENCERRAR") { dialog, _ ->
                        dialog.dismiss()
                        viewModel.endMatch()
                        NavigationUtils.navigateToMenu(this)
                    }
                    .create().show()
            }
        } else {
            binding.closeButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Sair da partida?")
                    .setNegativeButton("SAIR") { dialog, _ ->
                        dialog.dismiss()
                        viewModel.leaveMatch()
                        NavigationUtils.navigateToMenu(this)
                    }
                    .create().show()
            }
        }

        viewModel.actionable.observe(this) {
            if (it != null) {
                processActionable(it)
            }
        }

        viewModel.hostOnline.observe(this) {
            if (it == false) { ceaseMatch() }
        }

        binding.header.text = viewModel.matchId
    }

    private fun toastUnknownMessage(message: String) {
        Toast.makeText(
            this,
            "Mensagem desconhecida: $message",
            Toast.LENGTH_SHORT
        ).show()
    }

    //
    // Loading stuff
    //
    private fun updateLoadingHint(hint: String) {
        binding.loadingHint.animate()
            .alpha(0f)
            .setDuration(550)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.loadingHint.text = hint
                        binding.loadingHint.animate()
                            .alpha(1f)
                            .setDuration(550)
                            .setListener(null)
                            .start()
                    }
                }
            )
            .start()
    }

    private suspend fun animateGClef() {
        if (!isGclefAnimated) {
            isGclefAnimated = true

            while (isGclefAnimated) {
                delay(500)
                if (binding.gclef.rotation == 22f) {
                    binding.gclef.rotation = -15f
                } else {
                    binding.gclef.rotation = 22f
                }
            }
        }
    }

    private fun showLoadingOverlays(visible: Boolean) {
        if (visible) {
//            binding.mainContent.visibility = INVISIBLE
//            binding.currentChallengeHint.visibility = INVISIBLE

            lifecycleScope.launch { animateGClef() }
            binding.gclef.visibility = VISIBLE
            binding.gclef.animate()
                .alpha(1f)
                .setDuration(550)
                .start()

            binding.loadingHint.visibility = VISIBLE
            binding.loadingHint.animate()
                .alpha(1f)
                .setDuration(750)
                .start()

        } else {
            binding.gclef.visibility = GONE
            binding.loadingHint.visibility = GONE
//            binding.currentChallengeHint.visibility = INVISIBLE
//            binding.mainContent.visibility = INVISIBLE
            isGclefAnimated = false
        }
    }

    private fun gracefullyHideLoading() {
        binding.gclef.animate()
            .alpha(0f)
            .setDuration(1000)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.gclef.visibility = GONE
                    }
                }
            )
            .start()
        binding.loadingHint.animate()
            .alpha(0f)
            .setDuration(1000)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.loadingHint.visibility = GONE
                    }
                }
            )
            .start()
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

    private fun Int.fromDpToPx() = (this * resources.displayMetrics.density).toInt()
}