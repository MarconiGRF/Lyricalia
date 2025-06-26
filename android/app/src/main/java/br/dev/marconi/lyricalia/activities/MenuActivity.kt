package br.dev.marconi.lyricalia.activities

import android.animation.ValueAnimator
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivityMenuBinding
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.StorageUtils
import br.dev.marconi.lyricalia.viewModels.MenuViewModel
import br.dev.marconi.lyricalia.viewModels.MenuViewModelFactory
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Float

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding
    private lateinit var viewModel: MenuViewModel

    private var isGclefAnimated = false
    private var animator: ValueAnimator? = null

    override fun onResume() {
        super.onResume()

        viewModel.currentGreeting = viewModel.greetingPhrases.random()
        setupGreeting()
    }

    override fun onStart() {
        super.onStart()

        showLoadingOverlays(false)

        if (viewModel.currentUser.value != null) {
            viewModel.currentUser.value!!.spotifyToken?.run {
                setupMenuUI()
            } ?: NavigationUtils.navigateToSpotifyLink(this)

            if (!viewModel.currentUser.value!!.isLibraryProcessed) {
                followLibraryProcessing()
                showLoadingOverlays(true)
            }
        }
    }

    private fun setupMenuUI() {
        setupGreeting()
        setupLogoutButton()

        binding.createMatchButton.setOnClickListener {
            NavigationUtils.navigateToMatchCreate(this)
        }
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

    private fun updateProgressBar(percentage: Float) {
        animator = ValueAnimator.ofFloat(binding.animatedProgressBar.scaleX, percentage).apply {
            duration = 250

            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                binding.animatedProgressBar.scaleX = scale
            }

            start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = MenuViewModelFactory(applicationContext.filesDir)
        viewModel = ViewModelProvider(this, vmFactory)[MenuViewModel::class.java]

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setupMenuActivity()

        binding.animatedProgressBar.scaleX = 0f
    }

    private fun showLoadingOverlays(visible: Boolean) {
        if (visible) {
            binding.animatedProgressBar.visibility = VISIBLE
            binding.gclef.visibility = VISIBLE
            binding.loadingHint.visibility = VISIBLE
            binding.mainContent.visibility = INVISIBLE
            lifecycleScope.launch { animateGClef() }
        } else {
            binding.animatedProgressBar.visibility = INVISIBLE
            binding.gclef.visibility = INVISIBLE
            binding.loadingHint.visibility = INVISIBLE
            binding.mainContent.visibility = VISIBLE
            isGclefAnimated = false
        }
    }

    private fun followLibraryProcessing() {
        lifecycleScope.launch {
            viewModel.httpClient.webSocket(HttpMethod.Get, viewModel.serverIp, 8080, "/spotify/library") {
                try {
                    send(Frame.Text(viewModel.currentUser.value!!.id!!))

                    for (frame in incoming) {
                        showLoadingOverlays(true)
                        if (frame is Frame.Text) {
                            Log.d("IF1001_P3_LYRICALIA", "Websocket Frame Received -> ${String(frame.data)}")
                            val percentage = String(frame.data).toFloat()
                            updateProgressBar(percentage)
                        }
                        else if (frame is Frame.Close) {
                            Log.d("IF1001_P3_LYRICALIA", "Websocket closed: ${frame.readReason()}")
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("IF1001_P3_LYRICALIA", "Websocket exception: $ex")
                } finally {
                    if (closeReason.await()?.code == CloseReason.Codes.NORMAL.code) {
                        showLoadingOverlays(false)
                        binding.animatedProgressBar.visibility = INVISIBLE
                    }
                }
            }
        }
    }

    private fun setupMenuActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun setupGreeting() {
        val firstName = viewModel.currentUser.value!!.name.split(" ").first()

        val plainGreeting = viewModel.currentGreeting.first.replace("<USER>", firstName)

        val firstNameIndex = plainGreeting.indexOf(firstName)
        binding.greeting.text = SpannableString(plainGreeting).also {
            it.setSpan(
                StyleSpan(Typeface.ITALIC),
                0,
                plainGreeting.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            it.setSpan(
                StyleSpan(Typeface.BOLD),
                firstNameIndex,
                firstNameIndex + firstName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.greeting.setOnClickListener {
            Toast.makeText(this, viewModel.currentGreeting.second, Toast.LENGTH_LONG).show()
        }
    }

    fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        StorageUtils(applicationContext.filesDir).deleteUser()
        NavigationUtils.navigateToLogin(this)
    }
}