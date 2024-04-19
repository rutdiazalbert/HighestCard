package com.example.highestcardwingame

import DBHelper
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.random.Random

class GameActivity : AppCompatActivity() {
    private lateinit var ivCard1: ImageView
    private lateinit var ivCard2: ImageView
    private lateinit var pointBot: TextView
    private lateinit var pointPlayer: TextView
    private lateinit var btnDeal: Button
    private lateinit var btnRanking: BottomNavigationView
    private lateinit var dbHelper: DBHelper
    private lateinit var name: String
    private lateinit var title: TextView
    private lateinit var totalPoints: TextView
    private var player = 0
    private var casino = 0
    private var dealBet = 0
    private val cardArray = intArrayOf(
        R.drawable.h1, R.drawable.h2, R.drawable.h3, R.drawable.h4,
        R.drawable.h5, R.drawable.h6, R.drawable.h7, R.drawable.h8,
        R.drawable.h9, R.drawable.h10, R.drawable.hj, R.drawable.hq, R.drawable.hk
    )
    private val random = Random

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DBHelper(this, null)

        name = intent.getStringExtra("PLAYER").toString()
        setupViews()
        showBetDialModal()
        btnRanking.selectedItemId = R.id.btn_play;
        btnRanking.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btn_play -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.btn_ranking -> {
                    startActivity(Intent(this, RankingActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        btnDeal.setOnClickListener {
            if (btnDeal.text == "NEW GAME") {
                manageEndGame(player == dealBet)
            } else {
                playGame()
            }
        }
    }

    private fun setupViews() {
        pointPlayer = findViewById(R.id.point_player)
        pointBot = findViewById(R.id.point_bot)
        ivCard1 = findViewById(R.id.iv_card1)
        ivCard2 = findViewById(R.id.iv_card2)
        btnDeal = findViewById(R.id.btn_deal)
        title = findViewById(R.id.title_game)
        totalPoints = findViewById(R.id.total_player_points)

        pointPlayer.text = "$name: 0"
        val playerObj = dbHelper.getPlayerByName(name)
        playerObj?.moveToFirst()
        val currentPlayerPoints = playerObj?.getInt(playerObj.getColumnIndexOrThrow(DBHelper.POINTS_COL)) ?: 0
        totalPoints.text = "Total points: $currentPlayerPoints"

        ivCard1.setImageResource(R.drawable.back)
        ivCard2.setImageResource(R.drawable.back)

        pointBot.text = "Casino: 0"
        title.text = "HIGHEST CARD BATTLE"

        btnRanking = findViewById(R.id.bottomNavigationView)
    }

    private fun showBetDialModal() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.play_deal_modal)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnf: Button = dialog.findViewById(R.id.btn_f)
        val btnt: Button = dialog.findViewById(R.id.btn_t)
        val btnft: Button = dialog.findViewById(R.id.btn_ft)

        btnf.setOnClickListener {
            dealBet = 5
            dialog.dismiss()
        }
        btnt.setOnClickListener {
            dealBet = 10
            dialog.dismiss()
        }
        btnft.setOnClickListener {
            dealBet = 15
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun playGame() {
        val card1 = random.nextInt(cardArray.size)
        val card2 = random.nextInt(cardArray.size)

        setCardImage(card1, ivCard1)
        setCardImage(card2, ivCard2)

        when {
            card1 > card2 -> {
                casino++
                pointBot.text = "Casino: $casino"
                title.text = "Casino wins"
                if (casino == dealBet) {
                    title.text = "CASINO WON THE GAME"
                    btnDeal.text = "NEW GAME"
                }
            }
            card1 < card2 -> {
                player++
                pointPlayer.text = "$name: $player"
                title.text = "You win!!"
                if (player == dealBet) {
                    title.text = "YOU WON THE GAME"
                    btnDeal.text = "NEW GAME"
                }
            }
            else -> title.text = "Tie!"
        }
    }

    private fun setCardImage(number: Int, image: ImageView) {
        image.setImageResource(cardArray[number])
    }

    private fun manageEndGame(playerWin: Boolean) {
        val playerObj = dbHelper.getPlayerByName(name)
        var playerPoints = 0
        var playerGames = 0
        playerObj?.apply {
            moveToFirst()
            playerPoints = getInt(getColumnIndexOrThrow(DBHelper.POINTS_COL))
            playerGames = getInt(getColumnIndexOrThrow(DBHelper.GAMES_COL))

            if (playerWin) {
                playerPoints += dealBet
            } else {
                playerPoints -= dealBet
            }
            playerGames++

            dbHelper.updatePlayer(name, playerGames, playerPoints)
        }

        casino = 0
        player = 0
        dealBet = 0
        title.text = "HIGHEST CARD BATTLE"
        pointBot.text = "Casino: $casino"
        pointPlayer.text = "$name: $player"
        btnDeal.text = "Play"
        totalPoints.text = "Total points: $playerPoints"
        ivCard1.setImageResource(R.drawable.back)
        ivCard2.setImageResource(R.drawable.back)
        showBetDialModal()
    }
}