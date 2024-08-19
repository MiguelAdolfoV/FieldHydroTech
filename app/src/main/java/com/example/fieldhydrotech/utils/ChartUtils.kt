package com.example.fieldhydrotech.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.fieldhydrotech.repo.DatabaseHelper
import com.example.fieldhydrotech.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChartUtils(private val context: Context, private val dbHelper: DatabaseHelper) {

    fun setupBarChart(barChart: BarChart) {
        barChart.description.isEnabled = false
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setDrawGridLines(true)
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false
        barChart.animateY(1000)

        // Disable interactions
        barChart.setTouchEnabled(true)
        barChart.isDragEnabled = false
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        barChart.setDoubleTapToZoomEnabled(false)
        barChart.isHighlightPerTapEnabled = true
        barChart.isHighlightPerDragEnabled = false

        // Configure legend
        val legend = barChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 24f
        legend.textColor = ContextCompat.getColor(context, R.color.subtitle_color)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadWeeklyBarChartData(barChart: BarChart): Boolean {
        val logCursor = dbHelper.getThisWeeksLogs()
        val entries = mutableListOf<BarEntry>()
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // Initializing data array for each day of the week
        val weeklyData = DoubleArray(7)
        val weeklyCounts = IntArray(7)

        if (logCursor.count == 0) {
            logCursor.close()
            return false
        }

        while (logCursor.moveToNext()) {
            val date = logCursor.getString(logCursor.getColumnIndexOrThrow("date"))
            val data = logCursor.getDouble(logCursor.getColumnIndexOrThrow("data"))

            // Extract the day of the week from the date and map it to an index
            val dayOfWeek = LocalDateTime.parse(
                date,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ).dayOfWeek.value - 1 // 0=Monday, ..., 6=Sunday
            weeklyData[dayOfWeek] += data
            weeklyCounts[dayOfWeek] += 1
        }
        logCursor.close()

        // Create BarEntry objects with averaged values
        weeklyData.forEachIndexed { index, value ->
            val average = if (weeklyCounts[index] > 0) value / weeklyCounts[index] else 0.0
            entries.add(BarEntry(index.toFloat(), average.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Humidity")
        dataSet.color = ContextCompat.getColor(context, R.color.primary_color)
        val barData = BarData(dataSet)

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = 7 // Asegura 7 columnas en la cuadrícula
        barChart.invalidate()

        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadDailyBarChartData(barChart: BarChart): Boolean {
        val logCursor = dbHelper.getTodaysLogs()
        val entries = mutableListOf<BarEntry>()
        val labels = (0..23).map { it.toString() }

        // Initializing data array for each hour of the day
        val hourlyData = DoubleArray(24)
        val hourlyCounts = IntArray(24)

        if (logCursor.count == 0) {
            logCursor.close()
            return false
        }

        while (logCursor.moveToNext()) {
            val date = logCursor.getString(logCursor.getColumnIndexOrThrow("date"))
            val data = logCursor.getDouble(logCursor.getColumnIndexOrThrow("data"))

            // Extract the hour from the date and map it to an index
            val hour =
                LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).hour
            hourlyData[hour] += data
            hourlyCounts[hour] += 1
        }
        logCursor.close()

        // Create BarEntry objects with averaged values
        hourlyData.forEachIndexed { index, value ->
            val average = if (hourlyCounts[index] > 0) value / hourlyCounts[index] else 0.0
            entries.add(BarEntry(index.toFloat(), average.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Humidity")
        dataSet.color = ContextCompat.getColor(context, R.color.primary_color)
        val barData = BarData(dataSet)

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = 24 // Asegura 24 columnas en la cuadrícula
        barChart.invalidate()

        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadMonthlyBarChartData(barChart: BarChart): Boolean {
        val logCursor = dbHelper.getThisMonthsLogs()
        val entries = mutableListOf<BarEntry>()
        val labels = listOf("Week 1", "Week 2", "Week 3", "Week 4")

        // Initializing data array for each week of the month
        val weeklyData = DoubleArray(4)
        val weeklyCounts = IntArray(4)

        if (logCursor.count == 0) {
            logCursor.close()
            return false
        }

        while (logCursor.moveToNext()) {
            val date = logCursor.getString(logCursor.getColumnIndexOrThrow("date"))
            val data = logCursor.getDouble(logCursor.getColumnIndexOrThrow("data"))

            // Extract the week of the month from the date and map it to an index
            val dayOfMonth = LocalDateTime.parse(
                date,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ).dayOfMonth
            val weekOfMonth = ((dayOfMonth - 1) / 7).coerceIn(0, 3) // 0=Week 1, ..., 3=Week 4

            weeklyData[weekOfMonth] += data
            weeklyCounts[weekOfMonth] += 1
        }
        logCursor.close()

        // Create BarEntry objects with averaged values
        weeklyData.forEachIndexed { index, value ->
            val average = if (weeklyCounts[index] > 0) value / weeklyCounts[index] else 0.0
            entries.add(BarEntry(index.toFloat(), average.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Humidity")
        dataSet.color = ContextCompat.getColor(context, R.color.primary_color)
        val barData = BarData(dataSet)

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = 4 // Asegura 4 columnas en la cuadrícula
        barChart.invalidate()

        return true
    }
}
