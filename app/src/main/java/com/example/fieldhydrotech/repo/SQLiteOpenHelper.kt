package com.example.fieldhydrotech.repo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "fieldhydrotech.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_ANTENNAS = "Antennas"
        const val TABLE_NOTIFICATIONS = "Notifications"
        const val TABLE_LOG = "Log"

        // Antennas Table Columns
        private const val COLUMN_ANTENNAS_MAC_ADDRESS = "mac_address"
        private const val COLUMN_ANTENNAS_NAME = "name"
        private const val COLUMN_ANTENNAS_BATTERY_LIFE = "battery_life"

        // Notifications Table Columns
        private const val COLUMN_NOTIFICATIONS_ID = "ID"
        private const val COLUMN_NOTIFICATIONS_ANTENNA_MAC_ADDRESS = "antenna_mac_address"
        private const val COLUMN_NOTIFICATIONS_TYPE = "type"

        // Log Table Columns
        private const val COLUMN_LOG_ID = "ID"
        private const val COLUMN_LOG_ANTENNA_MAC_ADDRESS = "antenna_mac_address"
        private const val COLUMN_LOG_DATE = "date"
        private const val COLUMN_LOG_DATA = "data"

        // Create table SQL queries
        private const val CREATE_TABLE_ANTENNAS =
            "CREATE TABLE $TABLE_ANTENNAS (" +
                    "$COLUMN_ANTENNAS_MAC_ADDRESS VARCHAR UNIQUE PRIMARY KEY, " +
                    "$COLUMN_ANTENNAS_NAME VARCHAR, " +
                    "$COLUMN_ANTENNAS_BATTERY_LIFE VARCHAR)"

        private const val CREATE_TABLE_NOTIFICATIONS =
            "CREATE TABLE $TABLE_NOTIFICATIONS (" +
                    "$COLUMN_NOTIFICATIONS_ID VARCHAR PRIMARY KEY, " +
                    "$COLUMN_NOTIFICATIONS_ANTENNA_MAC_ADDRESS VARCHAR, " +
                    "$COLUMN_NOTIFICATIONS_TYPE VARCHAR, " +
                    "FOREIGN KEY($COLUMN_NOTIFICATIONS_ANTENNA_MAC_ADDRESS) REFERENCES $TABLE_ANTENNAS($COLUMN_ANTENNAS_MAC_ADDRESS))"

        private const val CREATE_TABLE_LOG =
            "CREATE TABLE $TABLE_LOG (" +
                    "$COLUMN_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_LOG_ANTENNA_MAC_ADDRESS VARCHAR, " +
                    "$COLUMN_LOG_DATE INTEGER, " +
                    "$COLUMN_LOG_DATA NUMERIC, " +
                    "FOREIGN KEY($COLUMN_LOG_ANTENNA_MAC_ADDRESS) REFERENCES $TABLE_ANTENNAS($COLUMN_ANTENNAS_MAC_ADDRESS))"

        // SQL queries for drop tables
        private const val DROP_TABLE_ANTENNAS = "DROP TABLE IF EXISTS $TABLE_ANTENNAS"
        private const val DROP_TABLE_NOTIFICATIONS = "DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS"
        private const val DROP_TABLE_LOG = "DROP TABLE IF EXISTS $TABLE_LOG"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        dropAndRecreateDatabase(db)
    }

    fun dropAndRecreateDatabase() {
        val db = this.writableDatabase
        dropAndRecreateDatabase(db)
    }

    private fun dropAndRecreateDatabase(db: SQLiteDatabase) {
        db.execSQL(DROP_TABLE_ANTENNAS)
        db.execSQL(DROP_TABLE_NOTIFICATIONS)
        db.execSQL(DROP_TABLE_LOG)
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_ANTENNAS)
        db.execSQL(CREATE_TABLE_NOTIFICATIONS)
        db.execSQL(CREATE_TABLE_LOG)
    }

    // CRUD Operations for Antennas table
    fun insertAntenna(macAddress: String, name: String, batteryLife: String): Boolean {
        writableDatabase.use { db ->
            val contentValues = ContentValues().apply {
                put(COLUMN_ANTENNAS_MAC_ADDRESS, macAddress)
                put(COLUMN_ANTENNAS_NAME, name)
                put(COLUMN_ANTENNAS_BATTERY_LIFE, batteryLife)
            }
            val result = db.insert(TABLE_ANTENNAS, null, contentValues)
            return result != -1L
        }
    }

    fun getAllMacAddresses(): List<String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT mac_address FROM $TABLE_ANTENNAS", null)
        val macAddresses = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                macAddresses.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return macAddresses
    }

    // CRUD Operations for Notifications table
    fun insertNotification(id: String, antennaMacAddress: String, type: String): Boolean {
        writableDatabase.use { db ->
            val contentValues = ContentValues().apply {
                put(COLUMN_NOTIFICATIONS_ID, id)
                put(COLUMN_NOTIFICATIONS_ANTENNA_MAC_ADDRESS, antennaMacAddress)
                put(COLUMN_NOTIFICATIONS_TYPE, type)
            }
            val result = db.insert(TABLE_NOTIFICATIONS, null, contentValues)
            return result != -1L
        }
    }

    fun getAllNotifications(): Cursor {
        val db = writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NOTIFICATIONS", null)
    }

    // CRUD Operations for Log table
    fun insertLog(antennaMacAddress: String, date: String, data: Int): Boolean {
        writableDatabase.use { db ->
            val contentValues = ContentValues().apply {
                put(COLUMN_LOG_ANTENNA_MAC_ADDRESS, antennaMacAddress)
                put(COLUMN_LOG_DATE, date)
                put(COLUMN_LOG_DATA, data)
            }
            val result = db.insert(TABLE_LOG, null, contentValues)
            return result != -1L
        }
    }

    fun getAllLogs(): Cursor {
        val db = writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_LOG", null)
    }

    // Method to get today's logs
    @RequiresApi(Build.VERSION_CODES.O)
    fun getTodaysLogs(): Cursor {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return getLogsByDateRange(today, today)
    }

    // Method to get this week's logs
    @RequiresApi(Build.VERSION_CODES.O)
    fun getThisWeeksLogs(): Cursor {
        val today = LocalDate.now()
        val startOfWeek = today.with(java.time.DayOfWeek.MONDAY).format(DateTimeFormatter.ISO_DATE)
        val endOfWeek = today.with(java.time.DayOfWeek.SUNDAY).format(DateTimeFormatter.ISO_DATE)
        return getLogsByDateRange(startOfWeek, endOfWeek)
    }

    // Method to get this month's logs
    @RequiresApi(Build.VERSION_CODES.O)
    fun getThisMonthsLogs(): Cursor {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1).format(DateTimeFormatter.ISO_DATE)
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).format(DateTimeFormatter.ISO_DATE)
        return getLogsByDateRange(startOfMonth, endOfMonth)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLogsByDateRange(startDate: String, endDate: String): Cursor {
        val db = writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_LOG WHERE date(date) BETWEEN date(?) AND date(?)", arrayOf(startDate, endDate))
    }

    // New method to truncate all tables
    fun truncateTables() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_ANTENNAS")
        db.execSQL("DELETE FROM $TABLE_NOTIFICATIONS")
        db.execSQL("DELETE FROM $TABLE_LOG")
        Log.d("DatabaseHelper", "All tables have been truncated")
    }

    // Method to get antennas with logs
    fun getAntennasWithLogs(): List<Antenna> {
        val antennas = mutableListOf<Antenna>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ANTENNAS", null)

        if (cursor.moveToFirst()) {
            do {
                val macAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANTENNAS_MAC_ADDRESS))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANTENNAS_NAME))
                val batteryLife = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANTENNAS_BATTERY_LIFE))

                val logsCursor = db.rawQuery("SELECT * FROM $TABLE_LOG WHERE $COLUMN_LOG_ANTENNA_MAC_ADDRESS = ?", arrayOf(macAddress))
                val logs = mutableListOf<LogEntry>()
                if (logsCursor.moveToFirst()) {
                    do {
                        val date = logsCursor.getString(logsCursor.getColumnIndexOrThrow(COLUMN_LOG_DATE))
                        val data = logsCursor.getFloat(logsCursor.getColumnIndexOrThrow(COLUMN_LOG_DATA))
                        logs.add(LogEntry(date, data))
                    } while (logsCursor.moveToNext())
                }
                logsCursor.close()

                antennas.add(Antenna(macAddress, name, batteryLife, logs))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return antennas
    }

}

data class Antenna(
    val macAddress: String,
    val name: String,
    val batteryLife: String,
    val logs: List<LogEntry>
)

data class LogEntry(
    val date: String,
    val data: Float
)
