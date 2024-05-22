package com.example.highestcardwingame

import DBHelper
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RankingActivity : AppCompatActivity() {
    private lateinit var btnPlay: BottomNavigationView
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ranking)
        setupViews()
        setupNavigation()
        checkAuthentication()
        firestore = FirebaseFirestore.getInstance()
        setupRankingTable()
    }

    private fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        btnPlay = findViewById(R.id.bottomNavigationView)
        btnPlay.selectedItemId = R.id.btn_ranking;
        btnPlay.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btn_play -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.btn_ranking -> true
                else -> false
            }
        }
    }

    private fun setupRankingTable() {
        val tableLayout = findViewById<TableLayout>(R.id.ranking_table)

        firestore.collection("players")
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val sortedPlayers = result.documents.map { doc ->
                    Triple(doc.getString("name") ?: "Unknown", doc.getLong("games")?.toInt() ?: 0, doc.getLong("points")?.toInt() ?: 0)
                }

                val headerRow = TableRow(this)
                val headerLayoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
                headerRow.layoutParams = headerLayoutParams

                val nombreHeader = TextView(this)
                nombreHeader.text = "Name"
                nombreHeader.setPadding(16, 16, 16, 16)
                nombreHeader.setTextColor(Color.WHITE)
                nombreHeader.setTextSize(1, 24F)

                val partidasHeader = TextView(this)
                partidasHeader.text = "Games"
                partidasHeader.setPadding(16, 16, 16, 16)
                partidasHeader.setTextColor(Color.WHITE)
                partidasHeader.setTextSize(1, 24F)

                val puntuacionHeader = TextView(this)
                puntuacionHeader.text = "Score"
                puntuacionHeader.setPadding(16, 16, 16, 16)
                puntuacionHeader.setTextColor(Color.WHITE)
                puntuacionHeader.setTextSize(1, 24F)

                headerRow.addView(nombreHeader)
                headerRow.addView(partidasHeader)
                headerRow.addView(puntuacionHeader)

                tableLayout.addView(headerRow)

                for ((i, jugador) in sortedPlayers.withIndex()) {
                    val row = TableRow(this)
                    val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
                    row.layoutParams = layoutParams

                    val nombreTextView = TextView(this)
                    nombreTextView.text = jugador.first
                    nombreTextView.setPadding(16, 16, 16, 16)
                    nombreTextView.setTextColor(Color.WHITE)
                    nombreTextView.setTextSize(1, 20F)

                    val partidasTextView = TextView(this)
                    partidasTextView.text = jugador.second.toString()
                    partidasTextView.setPadding(16, 16, 16, 16)
                    partidasTextView.setTextColor(Color.WHITE)
                    partidasTextView.setTextSize(1, 20F)

                    val puntuacionTextView = TextView(this)
                    puntuacionTextView.text = jugador.third.toString()
                    puntuacionTextView.setPadding(16, 16, 16, 16)
                    puntuacionTextView.setTextColor(Color.WHITE)
                    puntuacionTextView.setTextSize(1, 20F)

                    row.addView(nombreTextView)
                    row.addView(partidasTextView)
                    row.addView(puntuacionTextView)

                    tableLayout.addView(row, i + 1)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error getting rankings: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAuthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

}
