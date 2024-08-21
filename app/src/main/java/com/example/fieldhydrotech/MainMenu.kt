package com.example.fieldhydrotech

import AntennaAdapter
import MqttHelper
import WeatherIconUtils
import WeatherUtils
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fieldhydrotech.repo.DatabaseHelper
import com.example.fieldhydrotech.utils.ChartUtils
import com.example.fieldhydrotech.utils.NotificationUtils
import com.example.fieldhydrotech.repo.TestDataInserter
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
    private lateinit var notificationUtils: NotificationUtils
    private lateinit var testDataInserter: TestDataInserter
    private lateinit var mqttHelper: MqttHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var antennaAdapter: AntennaAdapter

    // Weather components
    private lateinit var weatherTextView: TextView
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
        notificationUtils = NotificationUtils()
        testDataInserter = TestDataInserter(dbHelper)

        // Weather components
        weatherTextView = findViewById(R.id.weather_text_view)
        weatherImageView = findViewById(R.id.weather_image_view)
        weatherUtil = WeatherUtils(this, "e81ea761176beda5398782787bb02340") // Reemplaza con tu API Key

        // Iniciar el servicio MQTT y establecer el listener
        mqttHelper = MqttHelper(this, dbHelper).apply {
            setMqttDataListener(this@MainMenu)
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
        val notificationBadge = findViewById<TextView>(R.id.toolbar_notification_badge)

        if (notificationBadge.text.toString().toIntOrNull() == 0) {
            notificationBadge.visibility = View.GONE
        } else {
            notificationBadge.visibility = View.VISIBLE
        }

        notificationButton.setOnClickListener {
            // Acción al hacer clic en el botón de notificaciones
            notificationUtils.updateNotificationCount(notificationBadge)
        }

        addButton.setOnClickListener {
            temporal++
        }

        // Cargar datos en el RecyclerView
        loadRecyclerViewData()

        // Iniciar la actualización periódica
        handler.post(updateRunnable)

        // Check and update weather
        checkLocationPermission()
        handler.post(weatherUpdateRunnable)
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
