package com.example.fieldhydrotech

import Notification
import NotificationManager
import WeatherIconUtils
import WeatherUtils
import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.fieldhydrotech.repo.DatabaseHelper
import com.example.fieldhydrotech.utils.QRCodeScannerUtils

class EmptyMenu : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var weatherUtil: WeatherUtils
    private lateinit var weatherTextView: TextView
    private lateinit var weatherImageView: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationContainer: LinearLayout


    private var qrResult: String? = null // Variable para almacenar el resultado del QR

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 10 * 60 * 1000L // 10 minutos en milisegundos
    private val weatherIconUtils = WeatherIconUtils(this)

    private val weatherUpdateRunnable = object : Runnable {
        override fun run() {
            updateWeatherData()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_empty_menu)

        // Configurar la interfaz de usuario para manejar los insets de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        weatherUtil = WeatherUtils(this, "e81ea761176beda5398782787bb02340") // Reemplaza con tu API Key

        drawerLayout = findViewById(R.id.drawer_layout)
        notificationContainer = findViewById(R.id.notification_container)
        weatherTextView = findViewById(R.id.weather_text_view)
        weatherImageView = findViewById(R.id.weather_image_view)

        // Inicializar NotificationManager con el contenedor de notificaciones
        notificationManager = NotificationManager(this, notificationContainer)

        val largeAddButton = findViewById<Button>(R.id.large_add_button)
        val notificationButton = findViewById<Button>(R.id.toolbar_notification_button)

        largeAddButton.setOnClickListener {
            QRCodeScannerUtils.startQRScanner(this)
        }

        notificationButton.setOnClickListener {
            toggleDrawer()
        }

        createNotifications()

        checkLocationPermission()

        handler.post(weatherUpdateRunnable)
    }

    private fun createNotifications() {
        notificationManager.addNotification(Notification(R.drawable.tower_broadcast_solid, "Antenna Successfully registered"))
        notificationManager.addNotification(Notification(R.drawable.plant_flooded_solid, "Saturated Soil"))
        notificationManager.addNotification(Notification(R.drawable.plant_dry_solid, "Dry Soil"))
        notificationManager.addNotification(Notification(R.drawable.battery_quarter_solid, "Low Battery"))
        notificationManager.addNotification(Notification(R.drawable.rain_notification, "Rain Warning"))
        notificationManager.addNotification(Notification(R.drawable.warning_notification, "Weather Warning"))
        notificationManager.addNotification(Notification(R.drawable.tower_broadcast_solid_warning, "Antenna Previously Registered"))
    }

    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.notification_drawer))) {
            drawerLayout.closeDrawer(findViewById(R.id.notification_drawer))
        } else {
            drawerLayout.openDrawer(findViewById(R.id.notification_drawer))
        }
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
                weatherIconUtils.setWeatherIcon(weatherResponse.weather[0].description, weatherImageView)
            } ?: run {
                Log.e("EmptyMenu", "updateWeatherData: Failed to obtain weather data")
                weatherTextView.text = "Internet/Location is off"
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
                weatherTextView.text = "Location isn't permitted"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        QRCodeScannerUtils.handleActivityResult(requestCode, resultCode, data) { qrContent ->
            if (qrContent != null) {
                // Guardar el resultado del QR
                qrResult = qrContent
                // Mostrar el diálogo de entrada
                showInputDialog()
            } else {
                Log.d("QRCodeScanner", "QR scan cancelled or failed.")
            }
        }
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_input, null)
        builder.setView(dialogView)

        val alertDialog = builder.create()

        // Mostrar el diálogo
        alertDialog.show()

        // Configurar los botones del diálogo
        val saveButton = dialogView.findViewById<Button>(R.id.buttonSave)
        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)

        // Manejar el clic del botón "Save"
        saveButton.setOnClickListener {
            val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
            val inputValue = editText.text.toString()
            qrResult?.let { qr ->
                handleInput(qr, inputValue)
            }
            alertDialog.dismiss()
        }

        // Manejar el clic del botón "Cancel"
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        // Cambiar el color de fondo programáticamente (si es necesario)
        val dialogWindow = alertDialog.window
        dialogWindow?.setBackgroundDrawableResource(R.color.white) // Reemplaza con tu color deseado
    }

    private fun handleInput(qrResult: String, name: String) {
        // Manejar los valores aquí
        if (dbHelper.insertAntenna(qrResult, name, "100%")) {
            // Preparar el intent para la actividad MainMenu
            val intent = Intent(this, MainMenu::class.java).apply {
                putExtra("notification_message", "Antenna : $name Successfully registered!")
            }
            startActivity(intent)
            finish() // Opcional: Terminar la actividad actual si ya no es necesaria
        } else {
            // Manejar el caso en que la inserción falló
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}


