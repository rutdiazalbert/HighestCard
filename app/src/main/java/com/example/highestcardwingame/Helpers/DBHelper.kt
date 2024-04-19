import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY, " +
                NAME_COl + " TEXT," +
                POINTS_COL + " INT," +
                GAMES_COL + " INT" + ")")
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    fun addPlayer(name : String){
        val values = ContentValues()

        values.put(NAME_COl, name)
        values.put(POINTS_COL, 0)
        values.put(GAMES_COL, 0)

        val db = this.writableDatabase

        db.insert(TABLE_NAME, null, values)

        db.close()
    }

    fun getPlayers(): Cursor? {
        val db = this.readableDatabase

        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null)

    }

    fun getPlayerByName(nombre: String): Cursor? {
        val db = this.readableDatabase
        val query = "SELECT * FROM "+ TABLE_NAME +" WHERE " + NAME_COl + " = ?"
        return db.rawQuery(query, arrayOf(nombre))
    }

    fun checkPlayerExists(nombre: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM "+ TABLE_NAME +" WHERE " + NAME_COl + " = ?"
        val cursor = db.rawQuery(query, arrayOf(nombre))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun updatePlayer(nombre: String, partidas: Int, puntuacion: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(GAMES_COL, partidas)
            put(POINTS_COL, puntuacion)
        }
        return db.update(TABLE_NAME, values, NAME_COl + " = ?", arrayOf(nombre))
    }

    companion object{
        private val DATABASE_NAME = "HIGHEST_CARD"

        private val DATABASE_VERSION = 1

        val TABLE_NAME = "players"

        val ID_COL = "id"

        val NAME_COl = "name"

        val POINTS_COL = "points"

        val GAMES_COL = "games"
    }
}