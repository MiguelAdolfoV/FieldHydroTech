package com.example.fieldhydrotech.repo

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TestDataInserter(private val dbHelper: DatabaseHelper) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertTestData(onComplete: () -> Unit) {
        dbHelper.dropAndRecreateDatabase()

        val antennasData = listOf(
            Triple("70:B3:D5:67:70:FF:FF:21", "Antenna 1", 100),  // MAC verdadera
            Triple("00:11:22:33:44:56", "Antenna 2", 100),
            Triple("00:11:22:33:44:57", "Antenna 3", 100),
            Triple("00:11:22:33:44:58", "Antenna 4", 100),
            Triple("00:11:22:33:44:59", "Antenna 5", 100)
        )

        antennasData.forEach {
            val result = dbHelper.insertAntenna(it.first, it.second, it.third)
            println("Inserted antenna: $it, success: $result")
        }

        val notificationsData = listOf(
            Triple("1", "70:B3:D5:67:70:FF:FF:21", "Battery Low"),
            Triple("2", "70:B3:D5:67:70:FF:FF:21", "Connection Lost"),
            Triple("3", "00:11:22:33:44:56", "Battery Full"),
            Triple("4", "00:11:22:33:44:57", "Maintenance Required"),
            Triple("5", "00:11:22:33:44:58", "Connection Restored")
        )

        notificationsData.forEach {
            val result = dbHelper.insertNotification(it.first, it.second, it.third)
            println("Inserted notification: $it, success: $result")

        }

        val now = LocalDateTime.now()
        val logData = mutableListOf<List<Any>>()

        // Dato para cada hora del día de hoy
        for (hour in 0 until 24) {
            val timestamp = now.withHour(hour).withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            logData.add(listOf("70:B3:D5:67:70:FF:FF:21", timestamp, (Math.random() * 100)))
        }

        // Dato para cada día de la semana actual
        for (day in 0 until 7) {
            val timestamp = now.minusDays(day.toLong()).withHour(0).withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            logData.add(listOf("70:B3:D5:67:70:FF:FF:21", timestamp, (Math.random() * 100)))
        }

        // Dato para cada semana del mes actual
        for (week in 0 until 4) {
            val timestamp = now.minusWeeks(week.toLong()).withHour(0).withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            logData.add(listOf("70:B3:D5:67:70:FF:FF:21", timestamp, (Math.random() * 100)))
        }

        logData.forEach {
            val result = dbHelper.insertLog(it[0] as String, it[1] as String, it[2] as Int)
            println("Inserted log: $it, success: $result")
        }

        // Llamar al callback una vez que se completen todas las inserciones
        onComplete()
    }
}
