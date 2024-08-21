import android.content.Context
import android.widget.ImageView
import com.example.fieldhydrotech.R

class WeatherIconUtils(private val context: Context) {

    // Mapeo de las descripciones a las imágenes correspondientes
    private val weatherImageMap = mapOf(
        // Thunderstorm Group 2xx
        "thunderstorm with light rain" to R.drawable.thunderstorm,
        "thunderstorm with rain" to R.drawable.thunderstorm,
        "thunderstorm with heavy rain" to R.drawable.thunderstorm,
        "light thunderstorm" to R.drawable.thunderstorm,
        "thunderstorm" to R.drawable.thunderstorm,
        "heavy thunderstorm" to R.drawable.thunderstorm,
        "ragged thunderstorm" to R.drawable.thunderstorm,
        "thunderstorm with light drizzle" to R.drawable.thunderstorm,
        "thunderstorm with drizzle" to R.drawable.thunderstorm,
        "thunderstorm with heavy drizzle" to R.drawable.thunderstorm,

        // Drizzle Group 3xx
        "light intensity drizzle" to R.drawable.drizzle,
        "drizzle" to R.drawable.drizzle,
        "heavy intensity drizzle" to R.drawable.drizzle,
        "light intensity drizzle rain" to R.drawable.drizzle,
        "drizzle rain" to R.drawable.drizzle,
        "heavy intensity drizzle rain" to R.drawable.drizzle,
        "shower rain and drizzle" to R.drawable.drizzle,
        "heavy shower rain and drizzle" to R.drawable.drizzle,
        "shower drizzle" to R.drawable.drizzle,

        // Rain Group 5xx
        "light rain" to R.drawable.rain,
        "moderate rain" to R.drawable.rain,
        "heavy intensity rain" to R.drawable.rain,
        "very heavy rain" to R.drawable.rain,
        "extreme rain" to R.drawable.rain,
        "freezing rain" to R.drawable.rain,
        "light intensity shower rain" to R.drawable.rain,
        "shower rain" to R.drawable.rain,
        "heavy intensity shower rain" to R.drawable.rain,
        "ragged shower rain" to R.drawable.rain,

        // Snow Group 6xx
        "light snow" to R.drawable.snow,
        "snow" to R.drawable.snow,
        "heavy snow" to R.drawable.snow,
        "sleet" to R.drawable.snow,
        "light shower sleet" to R.drawable.snow,
        "shower sleet" to R.drawable.snow,
        "light rain and snow" to R.drawable.snow,
        "rain and snow" to R.drawable.snow,
        "light shower snow" to R.drawable.snow,
        "shower snow" to R.drawable.snow,
        "heavy shower snow" to R.drawable.snow,

        // Atmosphere Group 7xx
        "mist" to R.drawable.mist,
        "smoke" to R.drawable.atmosphere,
        "haze" to R.drawable.mist,
        "sand/dust whirls" to R.drawable.atmosphere,
        "fog" to R.drawable.mist,
        "sand" to R.drawable.atmosphere,
        "dust" to R.drawable.atmosphere,
        "volcanic ash" to R.drawable.atmosphere,
        "squalls" to R.drawable.atmosphere,
        "tornado" to R.drawable.atmosphere,

        // Clear Group 800
        "clear sky" to R.drawable.clear,

        // Clouds Group 80x
        "few clouds: 11-25%" to R.drawable.few_clouds, // 801
        "scattered clouds: 25-50%" to R.drawable.few_clouds, // 802
        "broken clouds: 51-84%" to R.drawable.few_clouds, // 803
        "overcast clouds: 85-100%" to R.drawable.clouds // 804
    )

    // Método para actualizar la imagen basada en la descripción del clima
    fun setWeatherIcon(description: String, imageView: ImageView) {
        val imageResId = weatherImageMap[description.toLowerCase()] ?: R.drawable.clear
        imageView.setImageResource(imageResId)
    }
}
