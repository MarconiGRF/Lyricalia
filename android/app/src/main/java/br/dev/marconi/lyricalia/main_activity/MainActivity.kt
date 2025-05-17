package br.dev.marconi.lyricalia.main_activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.submitUserButton.setOnClickListener {
            doLogin()
        }

        setupUsernameMask()
    }

    fun setupUsernameMask() {
        binding.usernameText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) {
                s?.let { editable ->
                    binding.usernameText.removeTextChangedListener(this)

                    val modified = editable.replace("@".toRegex(), "" )
                    binding.usernameText.setText("@$modified")
                    binding.usernameText.setSelection(modified.length + 1)

                    binding.usernameText.addTextChangedListener(this)
                }
            }
        })
    }

    fun doLogin() {
        lifecycleScope.launch {
            binding.isLoading = true
            delay(3000)
            binding.isLoading = false
        }
    }
}