package br.dev.marconi.lyricalia.activities.match

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.activities.MatchPlayers
import br.dev.marconi.lyricalia.activities.MenuActivity
import br.dev.marconi.lyricalia.databinding.ActivityMatchPodiumBinding
import br.dev.marconi.lyricalia.repositories.match.PlayerFinalPodium
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.StorageUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.image.DrawableImage
import java.util.concurrent.TimeUnit

class MatchPodiumActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchPodiumBinding
    private lateinit var matchPlayers: MatchPlayers
    private var podiumIndicatorIds: MutableList<Int> = mutableListOf()
    private var mediaPlayer: MediaPlayer? = null

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

        matchPlayers = intent.extras!!.getParcelable(
            NavigationUtils.MATCH_PLAYERS_PARAMETER_ID,
            MatchPlayers::class.java
        )!!
        val podiumList = Gson().fromJson<List<PlayerFinalPodium>>(
            intent.extras!!.getString(NavigationUtils.JSONIFIED_PODIUM_PARAMETER_ID)!!,
            object : TypeToken<List<PlayerFinalPodium>>() {}.type
        )

        binding.backButton.setOnClickListener { NavigationUtils.navigateToMenu(this) }

        buildPodium(podiumList)
        animatePodium(podiumList, podiumList.size - 1)
    }

    private fun buildPodium(podiumList: List<PlayerFinalPodium>) {
        podiumList.forEachIndexed { idx, podiumInfo ->
            val podiumIndicatorInstance = LayoutInflater.from(this)
                .inflate(R.layout.podium_indicator, binding.mainContent, false)
            podiumIndicatorInstance.id = View.generateViewId()
            podiumIndicatorIds.add(podiumIndicatorInstance.id)

            podiumIndicatorInstance.findViewById<TextView>(R.id.playerUsername).also {
                it.text =  matchPlayers.players[
                    matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)
                ].username.toString()
                it.textSize = 12.fromDpToPx().toFloat()
                it.alpha = 0f
            }
            podiumIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer).also {
                it.setColorFilter(matchPlayers.colors[matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)][1])
                it.translationX = -1000f
            }
            podiumIndicatorInstance.findViewById<View>(R.id.stick).also {
                it.setBackgroundColor(matchPlayers.colors[matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)][1])
                it.translationX = -1000f
            }
            podiumIndicatorInstance.findViewById<TextView>(R.id.playerScore).also {
                it.text = "0"
                it.textSize = 12.fromDpToPx().toFloat()
                it.translationX = -1000f
            }

            binding.mainContent.addView(podiumIndicatorInstance)
        }
    }

    private fun animatePodium(podiumList: List<PlayerFinalPodium>, idx: Int) {
        if (idx < 0) {
            val currentUser = StorageUtils(filesDir).retrieveUser()!!

            if (currentUser.id == podiumList[0].id) makeAParty()
            binding.brandingBackground
                .animate()
                .alpha(0.45f)
                .setListener(
                    object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) {
                        binding.backButton.animate().alpha(1f).setDuration(1000).start()
                    }}
                )
                .setDuration(4000)
                .start()

            return
        }

        lifecycleScope.launch {
            val podiumIndicatorInstance = binding.mainContent.findViewById<ConstraintLayout>(podiumIndicatorIds[idx])

            playSong(R.raw.drums)
            podiumIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer)
                .animate()
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX(
                    (idx * -(60.fromDpToPx())).toFloat()
                )
                .setDuration(2000)
                .start()
            podiumIndicatorInstance.findViewById<View>(R.id.stick)
                .animate()
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX(
                    (idx * -(60.fromDpToPx())).toFloat()
                )
                .setDuration(2000)
                .start()

            val scoreView = podiumIndicatorInstance.findViewById<TextView>(R.id.playerScore)
            scoreView.animate()
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX(
                    (idx * -(60.fromDpToPx())).toFloat()
                )
                .setDuration(2000)
                .start()

            ValueAnimator.ofInt(0, podiumList[idx].score)
                .apply {
                    duration = 2000
                    addUpdateListener { animator ->  scoreView.text = animator.animatedValue.toString() }
                    doOnEnd {
                        podiumIndicatorInstance.findViewById<TextView>(R.id.playerUsername).animate().alpha(1f).setDuration(500).start()
                        playSong(R.raw.plates)

                        lifecycleScope.launch {
                            delay(1000)
                            animatePodium(podiumList, idx - 1)
                        }
                    }
                    start()
                }
            scoreView.alpha = 1f
        }
    }

    private fun Int.fromDpToPx() = (this * resources.displayMetrics.density).toInt()

    private fun makeAParty() {
        playSong(R.raw.trumpets)

        binding.run {
            konfettiLeft.start(
                party = Party(
                    angle = 55,
                    shapes = listOf(
                        Shape.Circle,
                        Shape.Square,
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_a)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_apostrophe)!!, 8, 19)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_c)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_l)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_r)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_y)!!, 17, 22))
                    ),
                    size = listOf(Size.LARGE, Size(15, mass = 6f)),
                    position = Position.Relative(0.0, 0.0),
                    timeToLive = 3200,
                    colors = listOf(0x61e786, 0x5a5766, 0x9792e3, 0xE86161),
                    emitter = Emitter(duration = 18, TimeUnit.SECONDS).perSecond(40)
                )
            )
            konfettiRight.start(
                party = Party(
                    angle = 145,
                    shapes = listOf(
                        Shape.Circle,
                        Shape.Square,
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_a)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_apostrophe)!!, 8, 19)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_c)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_l)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_r)!!, 18, 22)),
                        Shape.DrawableShape(DrawableImage(ContextCompat.getDrawable(this@MatchPodiumActivity ,R.drawable.just_y)!!, 17, 22))
                    ),
                    size = listOf(Size.LARGE, Size(15, mass = 6f)),
                    position = Position.Relative(1.0, 0.0),
                    timeToLive = 3200,
                    colors = listOf(0x61e786, 0x5a5766, 0x9792e3, 0xE86161),
                    emitter = Emitter(duration = 18, TimeUnit.SECONDS).perSecond(40)
                )
            )
        }
    }

    private fun playSong(resId: Int) {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }

            player.reset()

            val afd = resources.openRawResourceFd(resId)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player.prepare() // or prepareAsync()
            player.start()

        } ?: run {
            mediaPlayer = MediaPlayer().apply {
                val afd = resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                start()
            }
        }
    }
}