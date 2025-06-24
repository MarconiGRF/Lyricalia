package br.dev.marconi.lyricalia.activities.match

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.dev.marconi.lyricalia.databinding.ActivityMatchCreateBinding

class MatchCreateActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchCreateBinding

    override fun onStart() {
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val vmFactory = MenuViewModelFactory(applicationContext.filesDir)
//        viewModel = ViewModelProvider(this, vmFactory)[MenuViewModel::class.java]

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
    }
}