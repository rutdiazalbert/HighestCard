package com.example.highestcardwingame

import DBHelper
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.CalendarContract
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.properties.Delegates
import kotlin.random.Random
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    private var playerPoints by Delegates.notNull<Int>()
    private var player = 0
    private var casino = 0
    private var dealBet = 0
    private val cardArray = intArrayOf(
        R.drawable.h1, R.drawable.h2, R.drawable.h3, R.drawable.h4,
        R.drawable.h5, R.drawable.h6, R.drawable.h7, R.drawable.h8,
        R.drawable.h9, R.drawable.h10, R.drawable.hj, R.drawable.hq, R.drawable.hk
    )
    private val random = Random
    private val songs = listOf(
        R.raw.one_piece1,
        R.raw.juego_tronos,
        R.raw.one_piece2
    )
    private var currentSongIndex = 0  // Índice de la canción actual
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var btnMusicControl: ImageButton
    private var isMusicPlaying: Boolean = false
    private lateinit var cameraSoundPlayer: MediaPlayer
    private val REQUEST_PERMISSION_CODE = 101
    private val WRITE_CALENDAR_PERMISSION_REQUEST = 101
    private lateinit var firestore: FirebaseFirestore
    private var pot = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        //Inicializar Firestore
        firestore = FirebaseFirestore.getInstance()
        initializeMusic()
        // Inicializar el reproductor de música
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex])

        // Obtener referencias a los botones de flecha
        val btnMusicPrev = findViewById<ImageButton>(R.id.btn_music_control_prev)
        val btnMusicNext = findViewById<ImageButton>(R.id.btn_music_control_post)

        // Asignar listener al botón de flecha arriba (anterior)
        btnMusicPrev.setOnClickListener {
            cambiarCancionAnterior()
        }

        // Asignar listener al botón de flecha abajo (siguiente)
        btnMusicNext.setOnClickListener {
            cambiarCancionSiguiente()
        }
        btnMusicControl = findViewById(R.id.btn_music_control)
        btnMusicControl.setOnClickListener {
            if (isMusicPlaying) {
                pauseMusic()
            } else {
                startMusic()
            }
            togglePlayPauseIcon()
        }

        // Solicita permisos si no están otorgados
        if (!checkPermission()) {
            requestPermission()
        } else {
            takeScreenshot()
        }
        val btnSaveScreenshot: ImageButton = findViewById(R.id.btn_save_screenshot)
        btnSaveScreenshot.setOnClickListener {
            takeScreenshot()
        }

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
                manageEndGame()
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
        playerPoints = playerObj?.getInt(playerObj.getColumnIndexOrThrow(DBHelper.POINTS_COL)) ?: 0
        totalPoints.text = "Total points: $playerPoints"

        ivCard1.setImageResource(R.drawable.back)
        ivCard2.setImageResource(R.drawable.back)

        pointBot.text = "Casino: 0"
        title.text = "HIGHEST CARD BATTLE"

        btnRanking = findViewById(R.id.bottomNavigationView)
        pot = dbHelper.getPotPoints()
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

        // WebView
        val webViewHelp: WebView = dialog.findViewById(R.id.webview_help)
        val btnInfo: ImageButton = dialog.findViewById(R.id.btn_info)

        // Configurar clic del botón de información
        btnInfo.setOnClickListener {
            webViewHelp.visibility = if (webViewHelp.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        webViewHelp.loadUrl("file:///android_asset/help.html")

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
                    saveResults(false)
                }
            }
            card1 < card2 -> {
                player++
                pointPlayer.text = "$name: $player"
                title.text = "You win!!"
                if (player == dealBet) {
                    title.text = "YOU WON THE GAME"
                    btnDeal.text = "NEW GAME"
                    pot = 0
                    saveResults(true)
                }
            }
            else -> title.text = "Tie!"
        }

        //Animaciones cartas
        val flipForwardAnimation = AnimationUtils.loadAnimation(this, R.anim.flip_forward)
        flipForwardAnimation.duration = 500
        //Sonido animaciones
        val flipSound = MediaPlayer.create(this, R.raw.naipe)

        // Configuración sonido
        flipForwardAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                flipSound.start()
            }
            override fun onAnimationEnd(animation: Animation?) {
            }
            override fun onAnimationRepeat(animation: Animation?) {
                flipSound.pause()
            }
        })
        ivCard1.startAnimation(flipForwardAnimation)
        ivCard2.startAnimation(flipForwardAnimation)
    }

    private fun checkGameEnd() {
        if (player == 5) {
            playSound(R.raw.victory)
            showToast("¡ENHORABUENA, Has ganado!")
            Handler().postDelayed({
                saveVictoryMessageToCalendar()
            }, 2000)
        } else if (casino == 5) {
            playSound(R.raw.defeat)
            showToast("¡LÁSTIMA... Has perdido!")
        }
    }

    private fun takeScreenshot() {
        cameraSoundPlayer.start()
        // Toma una captura de pantalla de la vista raíz
        val rootView = window.decorView.rootView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false

        // Guarda la captura de pantalla en la galería de imágenes
        try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "Screenshot_$timeStamp.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }
            val resolver = contentResolver
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUri?.let {
                val outputStream: OutputStream? = resolver.openOutputStream(it)
                outputStream?.let { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    stream.close()
                    showToast("Screenshot saved successfully")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to save screenshot")
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_PERMISSION_CODE
        )
    }

    private fun playSound(soundResource: Int) {
        val mediaPlayer = MediaPlayer.create(this, soundResource)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { player -> player.release() }
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    private fun setCardImage(number: Int, image: ImageView) {
        image.setImageResource(cardArray[number])
    }

    private fun saveResults(playerWin: Boolean) {
        val playerObj = dbHelper.getPlayerByName(name)
        var playerGames = 0
        playerObj?.apply {
            moveToFirst()
            playerPoints = getInt(getColumnIndexOrThrow(DBHelper.POINTS_COL))
            playerGames = getInt(getColumnIndexOrThrow(DBHelper.GAMES_COL))

            if (playerWin) {
                val potPoints = dbHelper.getPotPoints()
                playerPoints += dealBet + potPoints
                dbHelper.resetPot()
            } else {
                playerPoints -= dealBet
                dbHelper.updatePot(dealBet)
            }
            playerGames++

            dbHelper.updatePlayer(name, playerGames, playerPoints)
            dbHelper.updatePot(pot)
        }
        totalPoints.text = "Total points: $playerPoints"
        checkGameEnd()

        //Guarda los datos en Firestore
        savePlayerToFirestore(name, playerPoints)
    }

    private fun savePlayerToFirestore(playerName: String, playerPoints: Int) {
        val playerData = hashMapOf(
            "name" to playerName,
            "points" to playerPoints
        )

        firestore.collection("players")
            .document(playerName)
            .set(playerData)
            .addOnSuccessListener {
                Log.d("Firestore", "Player data successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing player data", e)
            }
    }

    private fun saveTopPlayersToFirestore() {
        val cursor = dbHelper.getPlayers()
        val playersList = mutableListOf<Map<String, Any>>()

        cursor?.apply {
            while (moveToNext()) {
                val name = getString(getColumnIndexOrThrow(DBHelper.NAME_COl))
                val points = getInt(getColumnIndexOrThrow(DBHelper.POINTS_COL))
                val playerData = mapOf(
                    "name" to name,
                    "points" to points
                )
                playersList.add(playerData)
            }
            close()
        }

        // Ordenar los jugadores por puntos y tomar los 10 primeros
        val topPlayers = playersList.sortedByDescending { it["points"] as Int }.take(10)

        topPlayers.forEach { player ->
            val playerName = player["name"] as String
            firestore.collection("players")
                .document(playerName)
                .set(player)
                .addOnSuccessListener {
                    Log.d("Firestore", "Top player data successfully written!")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error writing top player data", e)
                }
        }
    }

    private fun getCurrentLocation(): String {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProvider: String = LocationManager.NETWORK_PROVIDER
        try {
            val lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)
            if (lastKnownLocation != null) {
                val latitude = lastKnownLocation.latitude
                val longitude = lastKnownLocation.longitude
                return "Lat: $latitude, Long: $longitude"
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return "Ubicación desconocida"
    }
    private fun saveVictoryMessageToCalendar() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Guardar partida")
        alertDialogBuilder.setMessage("¿Quieres guardar esta partida en el calendario?")
        alertDialogBuilder.setPositiveButton("Sí") { _, _ ->
            // Código para guardar el evento en el calendario
            val calendar = Calendar.getInstance()
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Victoria en Highest Card Battle")
                .putExtra(CalendarContract.Events.DESCRIPTION, "¡Felicitaciones! Has ganado una partida en Highest Card Battle.")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.timeInMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendar.timeInMillis + (5 * 60 * 1000))
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, getCurrentLocation())
            startActivity(intent)
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun manageEndGame() {
        casino = 0
        player = 0
        dealBet = 0
        title.text = "HIGHEST CARD BATTLE"
        pointBot.text = "Casino: $casino"
        pointPlayer.text = "$name: $player"
        btnDeal.text = "Play"

        ivCard1.setImageResource(R.drawable.back)
        ivCard2.setImageResource(R.drawable.back)
        showBetDialModal()
    }

    private fun initializeMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.one_piece1)
        mediaPlayer.isLooping = true
        cameraSoundPlayer = MediaPlayer.create(this, R.raw.camera)
    }

    private fun startMusic() {
        mediaPlayer.start()
        isMusicPlaying = true
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        isMusicPlaying = false
    }

    private fun togglePlayPauseIcon() {
        if (isMusicPlaying) {
            btnMusicControl.setBackgroundResource(R.drawable.baseline_cancel_24)
        } else {
            btnMusicControl.setBackgroundResource(R.drawable.baseline_play_circle_outline_24)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isMusicPlaying) {
            mediaPlayer.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    private fun cambiarCancionAnterior() {
        if (currentSongIndex > 0) {
            currentSongIndex--
        } else {
            currentSongIndex = songs.size - 1
        }
        mediaPlayer.stop()
        mediaPlayer.release()
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex])
        mediaPlayer.start()
    }

    private fun cambiarCancionSiguiente() {
        if (currentSongIndex < songs.size - 1) {
            currentSongIndex++
        } else {
            currentSongIndex = 0
        }
        mediaPlayer.stop()
        mediaPlayer.release()
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex])
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        saveTopPlayersToFirestore()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_CALENDAR_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveVictoryMessageToCalendar()
            }
        }
    }
}