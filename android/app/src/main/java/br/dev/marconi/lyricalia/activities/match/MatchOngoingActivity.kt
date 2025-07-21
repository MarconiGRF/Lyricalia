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
import android.media.MediaPlayer
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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.activities.MatchPlayers
import br.dev.marconi.lyricalia.databinding.ActivityMatchOngoingBinding
import br.dev.marconi.lyricalia.enums.HostCommands
import br.dev.marconi.lyricalia.enums.MatchMessages
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

    private var isGclefAnimated = true
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
            MatchMessages.RECEIVABLE_FINAL_PODIUM -> goToFinalPodium(messageParts[2])
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

    private fun goToFinalPodium(jsonifiedPodium: String) {
        NavigationUtils.navigateToMatchPodium(this, jsonifiedPodium, matchPlayers)
    }

    private fun saveChallengeAnswer(jsonifiedAnswer: String) {
        viewModel.currentChallengeAnswer = Gson()
            .fromJson<List<String>>(jsonifiedAnswer, object : TypeToken<List<String>>() {}.type)

        val answerCard = LayoutInflater.from(this@MatchOngoingActivity).inflate(
            R.layout.answer_card, binding.answerCarousel, false
        ).apply {
            this.findViewById<TextView>(R.id.playerName).text = "Resposta certa"
            this.findViewById<TextView>(R.id.lyrics).text = viewModel.currentChallengeAnswer.joinToString("\n")
        }
        answerCard.background = ContextCompat.getDrawable(applicationContext, R.drawable.card_background_green)?.mutate()
        answerCard.id = View.generateViewId()

        binding.answerCarousel.addView(answerCard)
    }

    private fun processSubmitted(playerId: String) {
        val donePlayerIdx = matchPlayers.players.map{ it.id }.indexOf(playerId)
        if (donePlayerIdx == -1) {
            toastUnknownMessage("Player submitted but not found on indicators")
            return
        }

        val playerIndicatorInstance = binding.playerIndicators.findViewById<View>(matchPlayers.viewsId[donePlayerIdx])
        playerIndicatorInstance.animate()
            .translationY(-40.fromDpToPx().toFloat())
            .setDuration(100)
            .start()
        playerIndicatorInstance.findViewById<ImageView>(R.id.playerCheckmark).alpha = 1f

        MediaPlayer.create(applicationContext, R.raw.pop).start()
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
                    binding.header.text = "Respostas"
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
        binding.animatedProgressBar.animate().alpha(0f).setDuration(350).start()

        val podium = Gson().fromJson<List<PlayerPodium>>(jsonPodium, object : TypeToken<List<PlayerPodium>>() {}.type)
        lifecycleScope.launch {
            delay(1000)

            podium.forEach { podiumInfo ->
                val answerCard = LayoutInflater.from(this@MatchOngoingActivity).inflate(
                    R.layout.answer_card,
                    binding.answerCarousel,
                    false
                ).apply {
                    id = View.generateViewId()
                    background = ContextCompat.getDrawable(applicationContext, R.drawable.card_background)?.mutate()
                    this.findViewById<TextView>(R.id.playerName).text = matchPlayers.players[
                        matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)
                    ].name
                    this.findViewById<TextView>(R.id.lyrics).text = podiumInfo.submission.joinToString("\n")
                }
                binding.answerCarousel.addView(answerCard)
            }

            binding.answerCarouselScrollWrapper.visibility = VISIBLE
            binding.answerCarouselScrollWrapper.animate().alpha(1f).setDuration(800).start()
        }

        lifecycleScope.launch {
            binding.playerIndicators.visibility = GONE

            if (viewModel.currentChallengeIndex + 1 == viewModel.challengeSet!!.songs.size) {
                binding.iterateChallengeButton.visibility = VISIBLE
                binding.iterateChallengeButton.animate().alpha(1f).setDuration(800).start()
                binding.iterateChallengeButton.setOnClickListener { iterateChallenge() }

                return@launch
            }

            podium.forEachIndexed { idx, podiumInfo ->
                delay(1000)

                val scoreIndicator = LayoutInflater.from(this@MatchOngoingActivity).inflate(
                    R.layout.podium_indicator,
                    binding.answerCarousel,
                    false
                ).apply { id = View.generateViewId() }

                scoreIndicator.findViewById<TextView>(R.id.playerUsername).also {
                    it.text =  matchPlayers.players[
                        matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)
                    ].username.toString()
                }
                scoreIndicator.findViewById<ImageView>(R.id.playerGlyphContainer).also{
                    it.setColorFilter(matchPlayers.colors[matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)][1])
                    it.translationX = (idx * -(60.fromDpToPx())).toFloat()
                }
                scoreIndicator.findViewById<View>(R.id.stick).also {
                    it.setBackgroundColor(matchPlayers.colors[matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)][1])
                    it.translationX = (idx * -(60.fromDpToPx())).toFloat()
                }
                scoreIndicator.findViewById<TextView>(R.id.playerScore).also {
                    it.text = podiumInfo.score.toString()
                    it.translationX = (idx * -(60.fromDpToPx())).toFloat()
                }

                binding.playerScores.addView(scoreIndicator)
            }

            binding.playerScores.visibility = VISIBLE
            binding.playerScores.animate().alpha(1f).setDuration(800)
                .setListener(
                    object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) {
                        binding.iterateChallengeButton.visibility = VISIBLE
                        binding.iterateChallengeButton.animate().alpha(1f).setDuration(800).start()
                        binding.iterateChallengeButton.setOnClickListener { iterateChallenge() }
                    }}
                )
                .start()
        }
    }

    private fun iterateChallenge() {
        // If challenge is the final song, then go to popper screen

        binding.mainContent.removeAllViews()
        binding.mainContent.visibility = GONE

        binding.playerIndicators.removeAllViews()
        binding.playerIndicators.visibility = GONE

        challengeInputIds = mutableListOf<Int>()
        matchPlayers.viewsId = arrayListOf()

        binding.divider.visibility = INVISIBLE
        binding.header.text = ""

        // Animate
        binding.playerScores.removeAllViews()
        binding.playerScores.visibility = GONE
        binding.iterateChallengeButton.visibility = GONE

        binding.answerCarouselScrollWrapper.visibility = GONE
        binding.answerCarousel.removeAllViews()

        viewModel.hasSubmittedAnswer = false

        binding.currentChallengeHint.visibility = INVISIBLE
        binding.challengeHint.layoutParams = LinearLayout.LayoutParams(-2, -2).also {
            it.setMargins(0, 0, 0, 60.fromDpToPx())
            it.gravity = Gravity.CENTER
        }
        ConstraintSet().also {
            it.clone(binding.mainLayout)
            it.connect(binding.currentChallengeHint.id, ConstraintSet.BOTTOM, binding.mainLayout.id, ConstraintSet.BOTTOM)
            it.connect(binding.currentChallengeHint.id, ConstraintSet.TOP, binding.mainLayout.id, ConstraintSet.TOP)
            it.applyTo(binding.mainLayout)
        }

        viewModel.notifyReadinessToChallenge()
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
            MediaPlayer.create(applicationContext, R.raw.clock_tick).start()

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
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())

        binding.animatedProgressBar.background = ContextCompat.getDrawable(applicationContext, R.drawable.rectangle_shape_red)
        lifecycleScope.launch { updateProgressBar(1f) }
        binding.header.text = "TEMPO!"
        binding.header.setTextColor(resources.getColor(R.color.lyRed, theme))
        MediaPlayer.create(applicationContext, R.raw.bell).start()

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
        showLoadingOverlays(false)

        binding.submitButton.visibility = GONE
        binding.submitButton.isClickable = false

        binding.songNameHint.text = viewModel.challengeSet!!.songs[viewModel.currentChallengeIndex].name
        binding.artistHint.text = viewModel.challengeSet!!.songs[viewModel.currentChallengeIndex].artist

        binding.challengeIndex.text = "${viewModel.currentChallengeIndex + 1}/${viewModel.challengeSet!!.songs.size}"
        binding.challengeIndex.visibility = VISIBLE

        binding.challengeHint.alpha = 1f
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

        binding.animatedProgressBar.visibility = VISIBLE
        binding.animatedProgressBar.alpha = 1f
        binding.animatedProgressBar.background = ContextCompat.getDrawable(applicationContext, R.drawable.rectangle_shape)

        binding.divider.visibility = VISIBLE

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
            it.clone(binding.mainLayout)
            it.connect(
                binding.currentChallengeHint.id, ConstraintSet.TOP,
                binding.divider.id, ConstraintSet.BOTTOM
            )
            it.clear(binding.currentChallengeHint.id, ConstraintSet.BOTTOM)
            it.applyTo(binding.mainLayout)
        }

        binding.challengeHint.layoutParams = LinearLayout.LayoutParams(-2, 0).also {
            it.setMargins(0, 0, 0, 0)
            it.gravity = Gravity.TOP
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

        lifecycleScope.launch { animateGClef() }
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
        isGclefAnimated = true
        binding.gclef.visibility = VISIBLE
        binding.gclef.alpha = 1f

        binding.loadingHint.alpha = 1f
        binding.loadingHint.visibility = VISIBLE
        binding.loadingHint.text = hint
    }

    private suspend fun animateGClef() {
        while (isGclefAnimated) {
            delay(500)
            if (binding.gclef.rotation == 22f) {
                binding.gclef.rotation = -15f
            } else {
                binding.gclef.rotation = 22f
            }
        }
    }

    private fun showLoadingOverlays(visible: Boolean) {
        if (visible) {
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