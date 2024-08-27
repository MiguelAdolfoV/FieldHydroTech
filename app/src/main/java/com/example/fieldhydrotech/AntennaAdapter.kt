import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fieldhydrotech.AntennaViewHolder
import com.example.fieldhydrotech.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.fieldhydrotech.repo.Antenna

class AntennaAdapter(
    private var antennaList: MutableList<Antenna>  // Cambiado a MutableList para permitir la actualización
) : RecyclerView.Adapter<AntennaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AntennaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.antenna_card, parent, false)
        return AntennaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AntennaViewHolder, position: Int) {
        val antenna = antennaList[position]
        holder.antennaTitle.text = antenna.name
        updateBatteryIcon(holder, antenna.batteryLife.toInt()) // Asegúrate de pasar un valor entero

        // Obtener solo los últimos 10 registros
        val logsToDisplay = if (antenna.logs.size > 10) {
            antenna.logs.takeLast(10)
        } else {
            antenna.logs
        }

        val entries = logsToDisplay.mapIndexed { index, logEntry -> Entry(index.toFloat(), logEntry.data) }
        val dataSet = LineDataSet(entries, "Humidity")

        // Accediendo al recurso de color sin contexto
        val color = ResourcesCompat.getColor(holder.itemView.resources, R.color.primary_color, null)
        dataSet.color = color

        // Deshabilitar interacciones en el LineChart
        holder.lineChart.setTouchEnabled(false)
        holder.lineChart.isDragEnabled = false
        holder.lineChart.setScaleEnabled(false)
        holder.lineChart.setPinchZoom(false)
        holder.lineChart.setDoubleTapToZoomEnabled(false)
        holder.lineChart.isHighlightPerTapEnabled = false
        holder.lineChart.isHighlightPerDragEnabled = false

        // Eliminar la cuadrícula
        holder.lineChart.xAxis.setDrawGridLines(false)
        holder.lineChart.axisLeft.setDrawGridLines(false)
        holder.lineChart.axisRight.setDrawGridLines(false)

        // Eliminar el "description label"
        holder.lineChart.description.isEnabled = false

        val lineData = LineData(dataSet)
        holder.lineChart.data = lineData
        holder.lineChart.invalidate()
    }

    override fun getItemCount(): Int {
        return antennaList.size
    }

    // Método para actualizar los datos del RecyclerView
    fun updateData(newAntennaList: List<Antenna>) {
        antennaList.clear() // Limpiar la lista actual
        antennaList.addAll(newAntennaList) // Agregar los nuevos datos
        notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado
    }

    // Método para actualizar el icono de batería basado en el additionalValue
    private fun updateBatteryIcon(holder: AntennaViewHolder, batteryLife: Int) {
        val drawableRes = if (batteryLife >= 76) {
            R.drawable.battery_full_solid
        } else if (batteryLife in 51..75){
            R.drawable.battery_three_quarters_solid
        } else if (batteryLife in 26..50){
            R.drawable.battery_half_solid
        } else if (batteryLife in 1..25){
            R.drawable.battery_quarter_solid
        } else {
            R.drawable.battery_empty_solid
        }
        holder.batteryIcon.setImageDrawable(ResourcesCompat.getDrawable(holder.itemView.resources, drawableRes, null))
        Log.d("MainMenu", "BaterryIcon Updated")

    }
}
