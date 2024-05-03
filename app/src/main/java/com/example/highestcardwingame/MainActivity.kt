package com.example.highestcardwingame

import DBHelper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var playerName: EditText
    private lateinit var submitButton: Button
    private lateinit var menu: BottomNavigationView
    private lateinit var languageSpinner: Spinner

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
        languageSpinner = findViewById(R.id.language_spinner)

        // Configurar adaptador para el Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.languages,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            languageSpinner.adapter = adapter
        }
        // Manejar cambios de selección en el Spinner
        languageSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = parent.getItemAtPosition(position) as String
                setAppLanguage(selectedLanguage)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        })
    }

    private fun setAppLanguage(language: String) {
        val locale = when (language) {
            "English" -> Locale("en")
            "Español" -> Locale("es")
            "Català" -> Locale("ca")
            else -> Locale.getDefault()
        }

        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Actualizar los componentes de la interfaz de usuario que contienen texto
        playerName.hint = getString(R.string.name)
        submitButton.text = getString(R.string.submit)
        menu.menu.findItem(R.id.btn_play).title = getString(R.string.play)
        menu.menu.findItem(R.id.btn_ranking).title = getString(R.string.ranking)

        val playerTitle = findViewById<TextView>(R.id.textView)
        playerTitle.text = getString(R.string.player)

        // Notificar al usuario que el idioma ha cambiado si es necesario
        Toast.makeText(this, "Language changed to $language", Toast.LENGTH_SHORT).show()
    }

    private fun setupMenu() {
        menu = findViewById(R.id.bottomNavigationView)
        menu.selectedItemId = R.id.btn_play;
        menu.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btn_play -> true
                R.id.btn_ranking -> {
                    val intent = Intent(this, RankingActivity::class.java).apply {
                        putExtra("LANGUAGE", languageSpinner.selectedItem.toString())
                    }
                    startActivity(intent)
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
