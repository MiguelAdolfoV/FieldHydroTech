package com.example.fieldhydrotech

import AntennaAdapter
import MqttHelper
import Notification
import NotificationManager
import WeatherIconUtils
import WeatherUtils
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fieldhydrotech.repo.DatabaseHelper
import com.example.fieldhydrotech.utils.ChartUtils
import com.example.fieldhydrotech.repo.TestDataInserter
import com.example.fieldhydrotech.utils.QRCodeScannerUtils
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMenu : AppCompatActivity(), MqttHelper.MqttDataListener {

    private var temporal = 1
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var weeklyBarChart: BarChart
    private lateinit var dailyBarChart: BarChart
    private lateinit var monthlyBarChart: BarChart
    private lateinit var dailyPlaceholder: ProgressBar
    private lateinit var weeklyPlaceholder: ProgressBar
    private lateinit var monthlyPlaceholder: ProgressBar
    private lateinit var chartUtils: ChartUtils
    private lateinit var testDataInserter: TestDataInserter
    private lateinit var mqttHelper: MqttHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var antennaAdapter: AntennaAdapter
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationContainer: LinearLayout
    private var qrResult: String? = null


    private lateinit var weatherTextView: TextView
    private lateinit var notificationBadge: TextView
    private lateinit var weatherImageView: ImageView
    private lateinit var weatherUtil: WeatherUtils
    private val weatherIconUtils = WeatherIconUtils(this)
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 30000L // 30 segundos
    private val weatherUpdateInterval = 10 * 60 * 1000L // 10 minutos

    private val updateRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            loadChartData()
            handler.postDelayed(this, updateInterval)
        }
    }

    private val weatherUpdateRunnable = object : Runnable {
        override fun run() {
            updateWeatherData()
            handler.postDelayed(this, weatherUpdateInterval)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        weeklyBarChart = findViewById(R.id.weeklyBarChart)
        dailyBarChart = findViewById(R.id.dailyBarChart)
        monthlyBarChart = findViewById(R.id.monthlyBarChart)
        dailyPlaceholder = findViewById(R.id.dailyPlaceholder)
        weeklyPlaceholder = findViewById(R.id.weeklyPlaceholder)
        monthlyPlaceholder = findViewById(R.id.monthlyPlaceholder)
        chartUtils = ChartUtils(this, dbHelper)
        testDataInserter = TestDataInserter(dbHelper)
        notificationContainer = findViewById(R.id.notification_container)
        notificationManager = NotificationManager(this, notificationContainer)

        // Weather components
        weatherTextView = findViewById(R.id.weather_text_view)
        weatherImageView = findViewById(R.id.weather_image_view)
        weatherUtil = WeatherUtils(this, "e81ea761176beda5398782787bb02340") // Reemplaza con tu API Key

        // Iniciar el servicio MQTT y establecer el listener
        mqttHelper = MqttHelper(this, dbHelper).apply {
            setMqttDataListener(this@MainMenu)
        }

        // Obtener el mensaje de notificación del Intent
        val notificationMessage = intent.getStringExtra("notification_message")

        // Si hay un mensaje de notificación, agregarlo
        notificationMessage?.let {
            notificationManager.addNotification(Notification(R.drawable.warning_notification, it))
        }

        // Configurar RecyclerView y Adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        antennaAdapter = AntennaAdapter(mutableListOf())
        recyclerView.adapter = antennaAdapter


        // Cargar datos de las gráficas
        loadChartData()

        // Inicializar vistas
        val notificationButton = findViewById<Button>(R.id.toolbar_notification_button)
        val addButton = findViewById<Button>(R.id.toolbar_add_button)
        notificationBadge = findViewById(R.id.toolbar_notification_badge)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)

        notificationButton.setOnClickListener {
            toggleDrawer()

        }

        addButton.setOnClickListener {
            QRCodeScannerUtils.startQRScanner(this)
        }

        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
                if (notificationManager.hasNotifications()) {
                    // Hay notificaciones en el drawer
                    notificationBadge.visibility = View.VISIBLE
                } else {
                    // No hay notificaciones en el drawer
                    notificationBadge.visibility = View.GONE
                }
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })

        if (notificationManager.hasNotifications()) {
            // Hay notificaciones en el drawer
            notificationBadge.visibility = View.VISIBLE
        } else {
            // No hay notificaciones en el drawer
            notificationBadge.visibility = View.GONE
        }

        // Cargar datos en el RecyclerView
        loadRecyclerViewData()

        // Iniciar la actualización periódica
        handler.post(updateRunnable)

        // Check and update weather
        checkLocationPermission()
        handler.post(weatherUpdateRunnable)
    }

    private fun toggleDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val drawerView = findViewById<View>(R.id.notification_drawer)
        if (drawer.isDrawerOpen(drawerView)) {
            drawer.closeDrawer(drawerView)
        } else {
            drawer.openDrawer(drawerView)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainMenu", "checkLocationPermission: Requesting location permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            Log.d("MainMenu", "checkLocationPermission: Permission already granted")
            updateWeatherData()
        }
    }

    private fun updateWeatherData() {
        Log.d("MainMenu", "updateWeatherData: Fetching weather data")
        weatherUtil.getWeatherData { weatherResponse ->
            weatherResponse?.let {
                Log.d("MainMenu", "updateWeatherData: Weather data received: ${it.main.temp}°C, ${it.weather[0].description}")
                weatherTextView.text = "Temp: ${it.main.temp}°C\n${it.weather[0].description}"
                weatherIconUtils.setWeatherIcon(weatherResponse.weather[0].description, weatherImageView)
            } ?: run {
                Log.e("MainMenu", "updateWeatherData: Failed to obtain weather data")
                weatherTextView.text = "Internet/Location is off"
            }
        }
    }

    private fun handleInput(qrResult: String, name: String) {
        if (dbHelper.insertAntenna(qrResult, name, 100)) {
            notificationManager.addNotification(Notification(R.drawable.tower_broadcast_solid, "Antenna : $name Successfully registered"))
        } else {
            notificationManager.addNotification(Notification(R.drawable.tower_broadcast_solid_warning, "Antenna: $name Not Registered"))
        }
        notificationBadge = findViewById(R.id.toolbar_notification_badge)

        if (notificationManager.hasNotifications()) {
            // Hay notificaciones en el drawer
            notificationBadge.visibility = View.VISIBLE
        } else {
            // No hay notificaciones en el drawer
            notificationBadge.visibility = View.GONE
        }

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
        alertDialog.show()

        val saveButton = dialogView.findViewById<Button>(R.id.buttonSave)
        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)

        saveButton.setOnClickListener {
            val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
            val inputValue = editText.text.toString()
            qrResult?.let { qr ->
                handleInput(qr, inputValue)
            }
            alertDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        val dialogWindow = alertDialog.window
        dialogWindow?.setBackgroundDrawableResource(R.color.white) // Reemplaza con tu color deseado
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadRecyclerViewData() {
        CoroutineScope(Dispatchers.IO).launch {
            val antennaList = dbHelper.getAntennasWithLogs()
            withContext(Dispatchers.Main) {
                antennaAdapter.updateData(antennaList)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadChartData() {
        CoroutineScope(Dispatchers.IO).launch {
            val hasDailyData = chartUtils.loadDailyBarChartData(dailyBarChart)
            val hasWeeklyData = chartUtils.loadWeeklyBarChartData(weeklyBarChart)
            val hasMonthlyData = chartUtils.loadMonthlyBarChartData(monthlyBarChart)

            withContext(Dispatchers.Main) {
                chartUtils.setupBarChart(weeklyBarChart)
                chartUtils.setupBarChart(dailyBarChart)
                chartUtils.setupBarChart(monthlyBarChart)
                if (hasDailyData) {
                    dailyPlaceholder.visibility = View.GONE
                    dailyBarChart.visibility = View.VISIBLE
                } else {
                    dailyPlaceholder.visibility = View.VISIBLE
                    dailyBarChart.visibility = View.GONE
                }

                if (hasWeeklyData) {
                    weeklyPlaceholder.visibility = View.GONE
                    weeklyBarChart.visibility = View.VISIBLE
                } else {
                    weeklyPlaceholder.visibility = View.VISIBLE
                    weeklyBarChart.visibility = View.GONE
                }

                if (hasMonthlyData) {
                    monthlyPlaceholder.visibility = View.GONE
                    monthlyBarChart.visibility = View.VISIBLE
                } else {
                    monthlyPlaceholder.visibility = View.VISIBLE
                    monthlyBarChart.visibility = View.GONE
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("MainMenu", "onRequestPermissionsResult: Location permission granted")
                updateWeatherData()
            } else {
                Log.e("MainMenu", "onRequestPermissionsResult: Location permission denied")
                weatherTextView.text = "Location isn't permitted"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDataReceived() {
        runOnUiThread {
            loadRecyclerViewData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainMenu", "onDestroy: Removing update handler callbacks")
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(weatherUpdateRunnable)
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }
}
