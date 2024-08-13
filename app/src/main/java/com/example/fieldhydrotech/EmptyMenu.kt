package com.example.fieldhydrotech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fieldhydrotech.repo.DatabaseHelper

class EmptyMenu : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_empty_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)

        val largeAddButton = findViewById<Button>(R.id.large_add_button)
        dbHelper.truncateTables()

        largeAddButton.setOnClickListener {
            saveAntennaToDatabase("70:B3:D5:67:70:FF:FF:21", "Antenna 1", "100%")
            navigateToMainMenu()
        }

    }

    private fun saveAntennaToDatabase(macAdress: String, antennaName: String, batteryLife: String ){
        val success = dbHelper.insertAntenna(macAdress, antennaName, batteryLife)

        if (success) {
            // Log the successful insertion or show a toast/message if needed
            Log.d("AntennaRegister", "Antenna Registered")
        } else {
            // Handle the error case if needed
            Log.d("Error", "Antena Error")
        }
    }

    private fun navigateToMainMenu() {
        val intent = Intent(this, MainMenu::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

}
