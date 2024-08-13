import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fieldhydrotech.AntennaViewHolder
import com.example.fieldhydrotech.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.fieldhydrotech.repo.Antenna

class AntennaAdapter(private val antennaList: List<Antenna>) : RecyclerView.Adapter<AntennaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AntennaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.antenna_card, parent, false)
        return AntennaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AntennaViewHolder, position: Int) {
        val antenna = antennaList[position]
        holder.antennaTitle.text = antenna.name

        val entries = antenna.logs.mapIndexed { index, logEntry -> Entry(index.toFloat(), logEntry.data) }
        val dataSet = LineDataSet(entries, "Data")
        val lineData = LineData(dataSet)
        holder.lineChart.data = lineData
        holder.lineChart.invalidate()
    }

    override fun getItemCount(): Int {
        return antennaList.size
    }
}
