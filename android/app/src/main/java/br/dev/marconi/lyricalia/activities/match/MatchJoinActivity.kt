package br.dev.marconi.lyricalia.activities.match

import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.databinding.ActivityMatchJoinBinding
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.MatchJoinViewModel
import br.dev.marconi.lyricalia.viewModels.MatchJoinViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MatchJoinActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchJoinBinding
    private lateinit var viewModel: MatchJoinViewModel

    private var isGclefAnimating = false
    private suspend fun animateGClef() {
        if (!isGclefAnimating) {
            isGclefAnimating = true

            while (isGclefAnimating) {
                delay(500)
                if (binding.gclef.rotation == 22f) {
                    binding.gclef.rotation = -15f
                } else {
                    binding.gclef.rotation = 22f
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = MatchJoinViewModelFactory(applicationContext.filesDir)
        viewModel = ViewModelProvider(this, vmFactory)[MatchJoinViewModel::class.java]

        binding = ActivityMatchJoinBinding.inflate(layoutInflater)
        setupMatchJoinActivity()
    }

    private fun setupMatchJoinActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.joinMatchButton.setOnClickListener {
            var matchId = binding.matchIdEditText.text.toString()
            joinMatch(matchId)
        }

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.matchIdEditText.requestFocus()
    }

    private fun joinMatch(matchId: String) {
        showLoadingOverlays(true)

        val activityContext = this
        lifecycleScope.launch {
            try {
                val matchExists = viewModel.joinMatch(matchId)
                if (matchExists) NavigationUtils.navigateToMatchWaiting(activityContext, matchId, false)
                else showLoadingOverlays(false, true, false)
            }
            catch (_: Exception) { showLoadingOverlays(false, false, true) }
        }
    }

    private fun showLoadingOverlays(shouldBeShown: Boolean, notFound: Boolean = false, otherError: Boolean = false) {
        if (shouldBeShown) {
            binding.mainContent.visibility = INVISIBLE
            binding.errorHint.visibility = INVISIBLE

            binding.gclef.visibility = VISIBLE
            lifecycleScope.launch { animateGClef() }

            binding.loadingHint.visibility = VISIBLE
            binding.loadingHint.text = resources.getString(R.string.loading_hint_simpler)
        } else {
            binding.loadingHint.visibility = INVISIBLE
            binding.mainContent.visibility = VISIBLE

            binding.gclef.visibility = INVISIBLE
            isGclefAnimating = false

            if (notFound) {
                binding.errorHint.text = resources.getString(R.string.error_match_not_found)
                binding.errorHint.visibility = VISIBLE
            } else if (otherError) {
                binding.errorHint.text = resources.getString(R.string.error_match_cannot_join)
                binding.errorHint.visibility = VISIBLE
            }
            else binding.loadingHint.visibility = INVISIBLE
        }
    }
}