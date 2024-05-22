package com.example.highestcardwingame

import DBHelper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.SignInButton
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var playerName: EditText
    private lateinit var submitButton: Button
    private lateinit var menu: BottomNavigationView
    private lateinit var languageSpinner: Spinner
    private lateinit var auth: FirebaseAuth;
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInButton: SignInButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        setupViews()
        setupMenu()
        setupGoogleSignIn()

        auth = FirebaseAuth.getInstance()
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
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

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

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInButton = findViewById(R.id.sign_in_button)
        signInButton.setOnClickListener {
            signIn()
        }

        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseAuth.getInstance().signOut()
            googleSignInClient.signOut().addOnCompleteListener(this) {
                Toast.makeText(
                    this,
                    "Logout successfully. See you soon ${user?.displayName}!",
                    Toast.LENGTH_SHORT
                ).show()
            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "Logout failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "Already logged out, please Sign In first.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Authentication successful. Welcome ${user?.displayName}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
