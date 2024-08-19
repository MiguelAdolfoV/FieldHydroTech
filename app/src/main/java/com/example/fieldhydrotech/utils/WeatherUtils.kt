import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherUtils(private val context: Context, private val apiKey: String) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun getWeatherData(onWeatherDataReceived: (WeatherResponse?) -> Unit) {
        Log.d("WeatherUtils", "Checking location permissions")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("WeatherUtils", "Location permission not granted")
            onWeatherDataReceived(null)
            return
        }

        Log.d("WeatherUtils", "Fetching last known location")
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("WeatherUtils", "Location obtained: lat=${it.latitude}, lon=${it.longitude}")
                fetchWeather(it.latitude, it.longitude, onWeatherDataReceived)
            } ?: run {
                Log.e("WeatherUtils", "Failed to obtain location")
                onWeatherDataReceived(null)
            }
        }.addOnFailureListener {
            Log.e("WeatherUtils", "Error obtaining location", it)
            onWeatherDataReceived(null)
        }
    }

    private fun fetchWeather(latitude: Double, longitude: Double, onWeatherDataReceived: (WeatherResponse?) -> Unit) {
        Log.d("WeatherUtils", "Fetching weather data from API")
        val weatherService = RetrofitInstance.api
        val call = weatherService.getCurrentWeatherByCoordinates(latitude, longitude, apiKey)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    Log.d("WeatherUtils", "Weather data fetched successfully")
                    onWeatherDataReceived(response.body())
                } else {
                    Log.e("WeatherUtils", "API response was not successful: ${response.code()} ${response.message()}")
                    onWeatherDataReceived(null)
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherUtils", "API call failed", t)
                onWeatherDataReceived(null)
            }
        })
    }
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val api: WeatherService by lazy {
        Log.d("RetrofitInstance", "Initializing Retrofit for WeatherService")
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }
}
