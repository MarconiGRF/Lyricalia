package br.dev.marconi.lyricalia.activities.match

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.dev.marconi.lyricalia.databinding.ActivityMatchPodiumBinding

class MatchPodiumActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchPodiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMatchPodiumBinding.inflate(layoutInflater)
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
    }
}