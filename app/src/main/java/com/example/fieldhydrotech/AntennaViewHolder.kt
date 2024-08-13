package com.example.fieldhydrotech

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart

class AntennaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val antennaTitle: TextView = itemView.findViewById(R.id.antennaTitle)
    val batteryIcon: ImageView = itemView.findViewById(R.id.batteryIcon)
    val lineChart: LineChart = itemView.findViewById(R.id.antennaLineChart)
}
