package com.example.highestcardwingame

import DBHelper
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class RankingActivity : AppCompatActivity() {
    private lateinit var btnPlay: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ranking)
        setupViews()
        setupNavigation()
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
        val playersHelper = DBHelper(this, null)
        val cursor = playersHelper.getPlayers()

        cursor?.let {
            it.moveToPosition(-1)
            val sortedPlayers = mutableListOf<Triple<String, Int, Int>>()
            while (it.moveToNext()) {
                val nombre = it.getString(it.getColumnIndexOrThrow(DBHelper.NAME_COl))
                val puntuacion = it.getInt(it.getColumnIndexOrThrow(DBHelper.POINTS_COL))
                val partidas = it.getInt(it.getColumnIndexOrThrow(DBHelper.GAMES_COL))
                sortedPlayers.add(Triple(nombre, partidas, puntuacion))
            }
            sortedPlayers.sortByDescending { it.third } // Ordenar de mayor a menor puntuación
            val headerRow = TableRow(this)
            val headerLayoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            headerRow.layoutParams = headerLayoutParams

            val nombreHeader = TextView(this)
            nombreHeader.text = "Nombre"
            nombreHeader.setPadding(16, 16, 16, 16)
            nombreHeader.setTextColor(Color.WHITE)
            nombreHeader.setTextSize(1, 24F)

            val partidasHeader = TextView(this)
            partidasHeader.text = "Partidas"
            partidasHeader.setPadding(16, 16, 16, 16)
            partidasHeader.setTextColor(Color.WHITE)
            partidasHeader.setTextSize(1, 24F)

            val puntuacionHeader = TextView(this)
            puntuacionHeader.text = "Puntuación"
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

                tableLayout.addView(row, i + 1) // Añadir 1 para dejar espacio para la fila de encabezado
            }
        }
    }
}
