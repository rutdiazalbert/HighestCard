package com.example.highestcardwingame

import DBHelper
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var playerName: EditText
    private lateinit var submitButton: Button
    private lateinit var menu: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        setupViews()
        setupMenu()
    }

    private fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        playerName = findViewById(R.id.player_name)
        submitButton = findViewById(R.id.btn_submit)
        submitButton.setOnClickListener {
            callActivity()
        }
    }

    private fun setupMenu() {
        menu = findViewById(R.id.bottomNavigationView)
        menu.selectedItemId = R.id.btn_play;
        menu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btn_play -> true
                R.id.btn_ranking -> {
                    startActivity(Intent(this, RankingActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun callActivity() {
        val playerNameInput = playerName.text.toString()
        val db = DBHelper(this, null)

        if (!db.checkPlayerExists(playerNameInput)) {
            db.addPlayer(playerNameInput)
        }
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("PLAYER", playerNameInput)
        }
        startActivity(intent)
    }
}
