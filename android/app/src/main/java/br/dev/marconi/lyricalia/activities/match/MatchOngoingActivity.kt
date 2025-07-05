package br.dev.marconi.lyricalia.activities.match

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivityMatchOngoingBinding
import br.dev.marconi.lyricalia.enums.HostCommands
import br.dev.marconi.lyricalia.enums.MatchMessages
import br.dev.marconi.lyricalia.enums.PlayerMessages
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.match.MatchOngoingViewModel
import br.dev.marconi.lyricalia.viewModels.match.MatchOngoingViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        viewModel.connectAsPlayer()
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
            MatchMessages.RECEIVABLE_WAITING -> { updateLoadingHint("Esperando outros jogadores...") }
            MatchMessages.RECEIVABLE_PROCESSING -> { updateLoadingHint("Pensando nas letras...") }
            else -> { toastUnknownMessage("1 " + messageParts.joinToString("$")) }
        }
    }

    private fun setupCommonUI() {
        binding = ActivityMatchOngoingBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
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
            binding.mainContent.visibility = INVISIBLE

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
            binding.mainContent.visibility = VISIBLE
            isGclefAnimated = false
        }
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
}