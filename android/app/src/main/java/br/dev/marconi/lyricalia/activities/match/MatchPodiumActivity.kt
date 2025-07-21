package br.dev.marconi.lyricalia.activities.match

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.R
import br.dev.marconi.lyricalia.activities.MatchPlayers
import br.dev.marconi.lyricalia.databinding.ActivityMatchPodiumBinding
import br.dev.marconi.lyricalia.repositories.match.PlayerFinalPodium
import br.dev.marconi.lyricalia.utils.NavigationUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class MatchPodiumActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMatchPodiumBinding
    private lateinit var matchPlayers: MatchPlayers
    private var podiumIndicatorIds: MutableList<Int> = mutableListOf()

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

        binding.backButton.setOnClickListener { animatePodium(podiumList, podiumList.size - 1) }

        buildPodium(podiumList)
//        animatePodium(podiumList, podiumList.size - 1)
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
                it.alpha = 0f
            }
            podiumIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer).also {
                it.setColorFilter(matchPlayers.colors[matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)][1])
                it.translationX = -1000f
//                it.scaleX = 0f
            }
            podiumIndicatorInstance.findViewById<View>(R.id.stick).also {
                it.setBackgroundColor(matchPlayers.colors[matchPlayers.players.map{ it.id }.indexOf(podiumInfo.id)][1])
                it.translationX = -1000f
//                it.scaleX = 0f
            }
            podiumIndicatorInstance.findViewById<TextView>(R.id.playerScore).also {
                it.text = "0"
                it.translationX = -1000f
//                it.scaleX = 0f
            }

            binding.mainContent.addView(podiumIndicatorInstance)
        }
    }

    private fun animatePodium(podiumList: List<PlayerFinalPodium>, idx: Int) {
        if (idx < 0) { return }

        lifecycleScope.launch {
            val podiumIndicatorInstance = binding.mainContent.findViewById<ConstraintLayout>(podiumIndicatorIds[idx])
            podiumIndicatorInstance.findViewById<ImageView>(R.id.playerGlyphContainer)
                .animate()
                .translationX(
                    (idx * -(60.fromDpToPx())).toFloat()
                )
                .setDuration(2000)
                .start()
            podiumIndicatorInstance.findViewById<View>(R.id.stick)
                .animate()
                .translationX(
                    (idx * -(60.fromDpToPx())).toFloat()
                )
                .setDuration(2000)
                .start()

            val scoreView = podiumIndicatorInstance.findViewById<TextView>(R.id.playerScore)
            scoreView.animate()
                .translationX(
                    (idx * -(60.fromDpToPx())).toFloat()
                )
                .setDuration(2000)
                .start()

            ValueAnimator.ofInt(0, podiumList[idx].score)
                .apply {
                    duration = 2000
                    addUpdateListener { animator ->  scoreView.text = animator.animatedValue.toString() }
                    start()
                }
            scoreView.alpha = 1f

            podiumIndicatorInstance.findViewById<TextView>(R.id.playerUsername).animate().alpha(1f).setDuration(2000).start()

            animatePodium(podiumList, idx - 1)
        }
    }

    private fun Int.fromDpToPx() = (this * resources.displayMetrics.density).toInt()
}