package br.dev.marconi.lyricalia.activities.match

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.activities.MatchPlayers
import br.dev.marconi.lyricalia.databinding.ActivityMatchOngoingBinding
import br.dev.marconi.lyricalia.enums.HostCommands
import br.dev.marconi.lyricalia.enums.MatchMessages
import br.dev.marconi.lyricalia.enums.PlayerMessages
import br.dev.marconi.lyricalia.repositories.match.PlayerPodium
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.viewModels.match.MatchOngoingViewModel
import br.dev.marconi.lyricalia.viewModels.match.MatchOngoingViewModelFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Int

class MatchOngoingActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchOngoingBinding
    private lateinit var viewModel: MatchOngoingViewModel
    private lateinit var matchPlayers: MatchPlayers

    private var isGclefAnimated = false
    private var animator: ValueAnimator? = null
    private var challengeInputIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = MatchOngoingViewModelFactory(applicationContext.filesDir, lifecycleScope)
        viewModel = ViewModelProvider(this, vmFactory)[MatchOngoingViewModel::class.java]
        viewModel.matchId = intent.extras?.getString(NavigationUtils.MATCH_ID_PARAMETER_ID)!!
        viewModel.isHost = intent.extras?.getBoolean(NavigationUtils.IS_HOST_PARAMETER_ID)!!
        matchPlayers = intent.extras?.getParcelable(NavigationUtils.MATCH_PLAYERS_PARAMETER_ID, MatchPlayers::class.java)!!

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
            MatchMessages.RECEIVABLE_WAITING -> updateLoadingHint("ESPERANDO JOGADORES...")
            MatchMessages.RECEIVABLE_PROCESSING -> updateLoadingHint("LETRANDO...")
            MatchMessages.RECEIVABLE_ANSWER -> saveChallengeAnswer(messageParts[2])
            MatchMessages.RECEIVABLE_CHALLENGE -> processChallengeActionable(messageParts)
            MatchMessages.RECEIVABLE_COUNTDOWN -> processCountdown(messageParts[2])
            MatchMessages.RECEIVABLE_PODIUM -> showPodium(messageParts[2])
            MatchMessages.RECEIVABLE_SUBMITTED -> processSubmitted(messageParts[2])
            MatchMessages.RECEIVABLE_READY -> {
                updateLoadingHint("VAMOS LÁ!")
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

    private fun saveChallengeAnswer(jsonifiedAnswer: String) {
        viewModel.currentChallengeAnswer = Gson()
            .fromJson<List<String>>(jsonifiedAnswer, object : TypeToken<List<String>>() {}.type)

    }

    private fun processSubmitted(playerId: String) {
        val donePlayerIdx = matchPlayers.players.map{ it.id }.indexOf(playerId)
        if (donePlayerIdx == -1) { toastUnknownMessage("Player submitted but not found on indicators") }

        val playerIndicatorInstance = binding.playerIndicators.findViewById<View>(matchPlayers.viewsId[donePlayerIdx])
        val layoutParams = playerIndicatorInstance.layoutParams as LinearLayout.LayoutParams

        ValueAnimator.ofInt(0, 40.fromDpToPx()).apply {
            duration = 100
            addUpdateListener { animator ->
                layoutParams.bottomMargin = animator.animatedValue as Int
                playerIndicatorInstance.layoutParams = layoutParams
            }
            start()
        }
        playerIndicatorInstance.findViewById<ImageView>(R.id.playerCheckmark).alpha = 1f
    }

    private fun processChallengeActionable(messageParts: List<String>) {
        val challengeInfo = messageParts[2].toIntOrNull()
        when (challengeInfo) {
            null ->  toastUnknownMessage("null challenge actionable")
            in 0 .. 100 -> {
                buildPlayerIndicators()
                viewModel.currentChallengeIndex = challengeInfo
                showChallengeHint()
            }
            else -> toastUnknownMessage("2 " + messageParts.joinToString("$"))
        }
    }

    private fun showPodium(jsonPodium: String) {
        binding.header.animate().alpha(0f).setDuration(350)
            .setListener(
                object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) {
                    binding.header.text = "Pódio"
                    binding.header.setTextColor(resources.getColor(R.color.lyGray, theme))
                    binding.header.animate().alpha(1f).setDuration(350).setListener(null).start()
                }}
            ).start()

        binding.mainContent.animate().alpha(0f).setDuration(350)
            .setListener(
                object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) {
                    binding.mainContent.visibility = INVISIBLE
                    binding.mainContent.alpha = 1f
                }}
            ).start()
        binding.submitButton.animate().alpha(0f).setDuration(350)
            .setListener(
                object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) {
                    binding.submitButton.visibility = INVISIBLE
                    binding.submitButton.alpha = 1f
                }}
            ).start()
        binding.currentChallengeHint.animate().alpha(0f).setDuration(350)
            .setListener(
                object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) {
                    binding.currentChallengeHint.visibility = INVISIBLE
                    binding.currentChallengeHint.alpha = 1f
                }}
            ).start()

        binding.animatedProgressBar.alpha = 0f

        val podium = Gson().fromJson<List<PlayerPodium>>(jsonPodium, object : TypeToken<List<PlayerPodium>>() {}.type)

        binding.podiumLayout.addView(
            TextView(this@MatchOngoingActivity).apply {
                text = jsonPodium
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
        )
        binding.podiumLayout.visibility = VISIBLE
    }

    private fun processCountdown(rawCountdown: String) {
        val countdown = rawCountdown.split("/")
        if (countdown.size <= 1) { toastUnknownMessage("invalid countdown: "); return }

        val currentTime = countdown[1].toFloat()

        if (currentTime >= 0f) {
            val percentage = currentTime / countdown[0].toFloat()
            lifecycleScope.launch { updateProgressBar(percentage) }

            binding.header.alpha = 1f
            binding.header.text = countdown[1]

            if (!viewModel.hasSubmittedAnswer) {
                binding.submitButton.setBackgroundColor(resources.getColor(R.color.lyGreen, theme))
                binding.submitButton.setTextColor(resources.getColor(R.color.Black, theme))
                binding.submitButton.isClickable = true
                binding.submitButton.visibility = VISIBLE
            }

            if (currentTime == 0f) {
                processTimesUp()
            }
        }
    }

    private fun processTimesUp() {
        binding.animatedProgressBar.background = ContextCompat.getDrawable(applicationContext, R.drawable.rectangle_shape_red)
        lifecycleScope.launch { updateProgressBar(1f) }
        binding.header.text = "TEMPO!"
        binding.header.setTextColor(resources.getColor(R.color.lyRed, theme))

        if (!viewModel.hasSubmittedAnswer) {
            submitAnswer()
        }
    }

    private fun submitAnswer() {
        challengeInputIds.forEach {
            val challengeInput = binding.mainContent.findViewById<EditText>(it)
            challengeInput.clearFocus()
            challengeInput.isEnabled = false
        }

        binding.submitButton.isClickable = false
        binding.submitButton.setBackgroundColor(resources.getColor(R.color.lyGray, theme))
        binding.submitButton.setTextColor(resources.getColor(R.color.lyWhite, theme))

        viewModel.submitChallengeAnswer()
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

    @SuppressLint("SetTextI18n")
    private fun showChallengeHint() {
        binding.submitButton.visibility = GONE
        binding.submitButton.isClickable = false

        binding.songNameHint.text = viewModel.challengeSet!!.songs[viewModel.currentChallengeIndex].name
        binding.artistHint.text = viewModel.challengeSet!!.songs[viewModel.currentChallengeIndex].artist

        binding.challengeIndex.text = "${viewModel.currentChallengeIndex + 1}/${viewModel.challengeSet!!.songs.size}"
        binding.challengeIndex.visibility = VISIBLE

        binding.currentChallengeHint.alpha = 0f
        binding.currentChallengeHint.visibility = VISIBLE
        binding.currentChallengeHint.animate()
            .alpha(1f)
            .setDuration(550)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        lifecycleScope.launch { setupChallenge() }
                    }
                }
            )
            .start()
    }

    private fun buildPlayerIndicators() {
        matchPlayers.players.forEachIndexed { idx, player ->
            val playerIndicator = LayoutInflater.from(this@MatchOngoingActivity)
                .inflate(R.layout.player_indicator_simple, binding.playerIndicators, false)
            playerIndicator.id = View.generateViewId()

            playerIndicator.findViewById<TextView>(R.id.playerGlyph).also {
                it.setTextColor(matchPlayers.colors[idx][0])
                it.text = player.name.first().uppercase().toString()
            }
            playerIndicator.findViewById<ImageView>(R.id.playerGlyphContainer).setColorFilter(
                matchPlayers.colors[idx][1]
            )

            matchPlayers.viewsId.add(playerIndicator.id)
            binding.playerIndicators.addView(playerIndicator)
        }
        binding.playerIndicators.visibility = VISIBLE
    }

    private suspend fun setupChallenge() {
        //TODO: Check if this is really necessary or section are too slow between them due to animations
        delay(3500)

        fadeHints()
        buildChallengeFields()

        delay(1000)
        viewModel.notifyReadinessToInput()

        binding.divider.visibility = VISIBLE
        binding.animatedProgressBar.visibility = VISIBLE
        binding.playerIndicators.visibility = VISIBLE
        binding.mainContent.visibility = VISIBLE
        binding.submitButton.setOnClickListener { submitAnswer()  }
    }

    private fun buildChallengeFields() {
        var lyrics = viewModel
            .challengeSet?.challenges[
                viewModel
                    .challengeSet?.songs[
                        viewModel.currentChallengeIndex
                    ]?.spotifyId
            ]

        lyrics?.forEachIndexed { idx, _ ->
            val verseContainer = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                clipChildren = false
                clipToPadding = false
            }

            binding.mainContent.clipChildren = false
            binding.mainContent.clipToPadding = false

            var subsequentVerse: View
            if (!lyrics[idx].startsWith("lyChal_")) {
                subsequentVerse = TextView(this)
                subsequentVerse.id = View.generateViewId()
                subsequentVerse.text = lyrics[idx]
                subsequentVerse.setTextColor(resources.getColor(R.color.lyDarkerGray, theme))

                subsequentVerse.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            } else {
                subsequentVerse = EditText(this).apply {
                    id = View.generateViewId()
                    challengeInputIds.add(id)
                    maxLines = Int.MAX_VALUE

                    setSingleLine(false)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    setHorizontallyScrolling(false)
                    setBackgroundColor(Color.TRANSPARENT)
                    minHeight = 48.fromDpToPx()
                    setTextAppearance(R.style.BoldEditTextStyle)
                    setTypeface(ResourcesCompat.getFont(this@MatchOngoingActivity, R.font.domine), Typeface.BOLD)
                    setTextColor(resources.getColor(R.color.lyIndigo, theme))
                    onFocusChangeListener = View.OnFocusChangeListener({ _, hasFocus ->
                        if (!hasFocus) {
                            viewModel
                                .challengeSet!!
                                .challenges[
                                viewModel
                                    .challengeSet!!
                                    .songs[
                                    viewModel.currentChallengeIndex
                                ].spotifyId
                            ]!![idx] = this.text.toString()
                        }
                    })
                    setPadding(0, 0, 0, 4.fromDpToPx())
                }

                val layerDrawable = LayerDrawable(arrayOf(
                    Color.TRANSPARENT.toDrawable(),
                    resources.getColor(R.color.lyDarkerGray, theme).toDrawable()
                ))
                layerDrawable.setLayerInset(1, 0, 0, 0, 0)
                layerDrawable.setLayerHeight(1, 3.fromDpToPx())
                layerDrawable.setLayerGravity(1, Gravity.BOTTOM)
                subsequentVerse.background = layerDrawable

                val layoutParams = FrameLayout.LayoutParams(
                    resources.displayMetrics.widthPixels - 30.fromDpToPx(),
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                layoutParams.bottomMargin = 10.fromDpToPx()
                subsequentVerse.layoutParams = layoutParams
            }

            subsequentVerse.typeface = ResourcesCompat.getFont(this, R.font.domine)
            subsequentVerse.textSize = 20f
            subsequentVerse.textAlignment = TEXT_ALIGNMENT_CENTER
            subsequentVerse.elevation = 10f
            subsequentVerse.setPadding(10.fromDpToPx(), 0, 10.fromDpToPx(), 5.fromDpToPx())

            verseContainer.addView(subsequentVerse)

            if (idx == 0) {
                val openingQuotes = TextView(this).apply {
                    id = View.generateViewId()
                    typeface = ResourcesCompat.getFont(this@MatchOngoingActivity, R.font.domine)
                    setTypeface(this.typeface, Typeface.BOLD)
                    textSize = 38f
                    textAlignment = TEXT_ALIGNMENT_CENTER
                    setTextColor(resources.getColor(R.color.lyDarkerGray, theme))
                    text = "“"
                    isFocusable = false
                    isClickable = false
                    elevation = 11f

                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        20.fromDpToPx()
                    )
                }

                verseContainer.addView(openingQuotes)

                subsequentVerse.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        subsequentVerse.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        val params = openingQuotes.layoutParams as FrameLayout.LayoutParams
                        params.leftMargin = subsequentVerse.left - 8.fromDpToPx()
                        params.topMargin = subsequentVerse.top - 8.fromDpToPx()
                        openingQuotes.layoutParams = params
                    }
                })
            }

            if (idx == lyrics.size - 1) {
                val closingQuotes = TextView(this).apply {
                    id = View.generateViewId()
                    typeface = ResourcesCompat.getFont(this@MatchOngoingActivity, R.font.domine)
                    setTypeface(this.typeface, Typeface.BOLD)
                    textSize = 38f
                    textAlignment = TEXT_ALIGNMENT_CENTER
                    setTextColor(resources.getColor(R.color.lyDarkerGray, theme))
                    text = "”"
                    isFocusable = false
                    isClickable = false
                    elevation = 11f

                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        20.fromDpToPx()
                    ).apply {
                        gravity = Gravity.TOP or Gravity.END
                    }
                }

                verseContainer.addView(closingQuotes)

                subsequentVerse.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        subsequentVerse.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        val params = closingQuotes.layoutParams as FrameLayout.LayoutParams
                        params.rightMargin = verseContainer.width - subsequentVerse.right + 8.fromDpToPx()
                        params.topMargin = subsequentVerse.top - 8.fromDpToPx()
                        closingQuotes.layoutParams = params
                    }
                })
            }

            binding.mainContent.addView(verseContainer)
        }
    }

    private fun fadeHints() {
        binding.header.animate()
            .alpha(0f)
            .setDuration(250)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.header.text = "10"
                    }
                }
            )
            .start()

        binding.challengeHint.animate()
            .alpha(0f)
            .setDuration(350)
            .start()
        binding.divider.animate()
            .alpha(1f)
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                val cf = currentFocus

                if (cf != null && inputMethodManager.isActive(currentFocus)) {
                    InputMethodService().requestHideSelf(0)
                    cf.clearFocus()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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