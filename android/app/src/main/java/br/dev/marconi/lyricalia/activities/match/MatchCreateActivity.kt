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
import br.dev.marconi.lyricalia.databinding.ActivityMatchCreateBinding
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.match.MatchCreateViewModel
import br.dev.marconi.lyricalia.viewModels.match.MatchCreateViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MatchCreateActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchCreateBinding
    private lateinit var viewModel: MatchCreateViewModel

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

        val vmFactory = MatchCreateViewModelFactory(applicationContext.filesDir)
        viewModel = ViewModelProvider(this, vmFactory)[MatchCreateViewModel::class.java]

        binding = ActivityMatchCreateBinding.inflate(layoutInflater)
        setupMatchCreateActivity()
    }

    private fun setupMatchCreateActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.createMatchButton.setOnClickListener {
            var songLimit = binding.songLimitEditText.text.toString().toInt()
            if (songLimit == 0) songLimit++

            createMatch(songLimit)
        }
    }

    private fun createMatch(songLimit: Int) {
        showLoadingOverlays(true)

        val activityContext = this
        lifecycleScope.launch {
            var matchId: String
            try {
                matchId = viewModel.createMatch(songLimit)
                NavigationUtils.navigateToMatchWaiting(activityContext, matchId, true)
            }
            catch (_: Exception) { showLoadingOverlays(false, true) }
        }
    }

    private fun showLoadingOverlays(shouldBeShown: Boolean, isError: Boolean = false) {
        if (shouldBeShown) {
            binding.mainContent.visibility = INVISIBLE

            binding.gclef.visibility = VISIBLE
            lifecycleScope.launch { animateGClef() }

            binding.loadingHint.visibility = VISIBLE
            binding.loadingHint.text = resources.getString(R.string.loading_hint_simpler)
        } else {
            binding.mainContent.visibility = VISIBLE

            binding.gclef.visibility = INVISIBLE
            isGclefAnimating = false

            if (isError) binding.loadingHint.text = resources.getString(R.string.error_match_creation)
            else binding.loadingHint.visibility = INVISIBLE
        }
    }
}