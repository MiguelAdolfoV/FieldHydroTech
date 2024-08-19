package com.example.fieldhydrotech

import WeatherUtils
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fieldhydrotech.repo.DatabaseHelper

class EmptyMenu : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var weatherUtil: WeatherUtils
    private lateinit var weatherTextView: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 10 * 60 * 1000L // 10 minutos en milisegundos

    private val weatherUpdateRunnable = object : Runnable {
        override fun run() {
            updateWeatherData()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_empty_menu)

        Log.d("EmptyMenu", "onCreate: Initializing EmptyMenu")

        // Configurar la interfaz de usuario para manejar los insets de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        weatherUtil = WeatherUtils(this, "e81ea761176beda5398782787bb02340") // Reemplaza con tu API Key

        weatherTextView = findViewById(R.id.weatherTextView)

        val largeAddButton = findViewById<Button>(R.id.large_add_button)

        largeAddButton.setOnClickListener {
            Log.d("EmptyMenu", "largeAddButton: Navigating to MainMenu")
            navigateToMainMenu()
        }

        checkLocationPermission()

        handler.post(weatherUpdateRunnable)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d("EmptyMenu", "checkLocationPermission: Requesting location permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            Log.d("EmptyMenu", "checkLocationPermission: Permission already granted")
            updateWeatherData()
        }
    }

    private fun updateWeatherData() {
        Log.d("EmptyMenu", "updateWeatherData: Fetching weather data")
        weatherUtil.getWeatherData { weatherResponse ->
            weatherResponse?.let {
                Log.d("EmptyMenu", "updateWeatherData: Weather data received: ${it.main.temp}°C, ${it.weather[0].description}")
                weatherTextView.text = "Temp: ${it.main.temp}°C\n${it.weather[0].description}"
            } ?: run {
                Log.e("EmptyMenu", "updateWeatherData: Failed to obtain weather data")
                weatherTextView.text = "No se pudo obtener el clima"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("EmptyMenu", "onRequestPermissionsResult: Location permission granted")
                updateWeatherData()
            } else {
                Log.e("EmptyMenu", "onRequestPermissionsResult: Location permission denied")
                weatherTextView.text = "Permiso de ubicación denegado"
            }
        }
    }

    private fun navigateToMainMenu() {
        val intent = Intent(this, MainMenu::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("EmptyMenu", "onDestroy: Removing weather update handler callbacks")
        handler.removeCallbacks(weatherUpdateRunnable)
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}
