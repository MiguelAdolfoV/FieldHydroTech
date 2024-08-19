package com.example.fieldhydrotech

import AntennaAdapter
import MqttHelper
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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

    private var temporal = 1 // Variable para simular si hay datos o no
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
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 30000L // 30 segundos

    private val updateRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            loadChartData()
            handler.postDelayed(this, updateInterval)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDataReceived() {
        runOnUiThread {
            loadRecyclerViewData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
}
