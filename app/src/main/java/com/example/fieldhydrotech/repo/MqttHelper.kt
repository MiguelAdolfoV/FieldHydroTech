import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.fieldhydrotech.repo.DatabaseHelper
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MqttHelper(context: Context, private val dbHelper: DatabaseHelper) {

    private val serverUri = "ssl://cran-gw.e-technik.tu-ilmenau.de:18330"
    private val clientId = "112311"
    private val username = "hemmecke"
    private val password = "#dgjhessw"
    private val subscriptionTopic = "mioty/70-b3-d5-67-70-0e-ff-ff/70-b3-d5-67-70-ff-03-33/uplinkDuplicate"

    private val mqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)

    private var mqttDataListener: MqttDataListener? = null

    init {
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                Log.d("Mqtt", "Connected to: $serverURI")
                subscribeToTopic()
            }

            override fun connectionLost(cause: Throwable) {
                Log.d("Mqtt", "The Connection was lost.")
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload, Charset.forName("UTF-8"))
                handleMessage(payload)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })
        connect()
    }

    fun setMqttDataListener(listener: MqttDataListener) {
        mqttDataListener = listener
    }

    private fun connect() {
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
        mqttConnectOptions.userName = username
        mqttConnectOptions.password = password.toCharArray()
        mqttConnectOptions.socketFactory = MqttClientUtil.getSocketFactory()

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("Mqtt", "Connected!")
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d("Mqtt", "Failed to connect to: $serverUri", exception)
                }
            })
        } catch (ex: MqttException) {
            Log.d("Mqtt", "Exception whilst trying to connect", ex)
            ex.printStackTrace()
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("Mqtt", "Subscribed!")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d("Mqtt", "Failed to subscribe")
                }
            })
        } catch (ex: MqttException) {
            Log.d("Mqtt", "Exception whilst subscribing")
            ex.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleMessage(payload: String) {
        val jsonnedMsg = JSONObject(payload)
        val baseStations = jsonnedMsg.getJSONArray("baseStations").getJSONObject(0)
        val bsEui = baseStations.getLong("bsEui")
        val rxTime = baseStations.getLong("rxTime")
        val cnt = jsonnedMsg.getInt("cnt")
        val dataArray = jsonnedMsg.getJSONArray("data")

        // Convertir el array de valores ASCII a un número
        val dataValue = convertAsciiArrayToNumber(dataArray)

        // Convertir bsEui a una dirección MAC
        val macAddress = convertBsEuiToMac(bsEui)

        // Convertir rxTime a una fecha y hora legible
        val dateTime = convertNanoTimeToDate(rxTime)

        // Mostrar el log con la dirección MAC del dispositivo y la fecha y hora legible
        Log.d("Mqtt", "Received message from MAC: $macAddress, rxTime: $dateTime, count: $cnt, data: $dataValue")

        // Obtener todas las direcciones MAC de la base de datos
        val registeredMacAddresses = dbHelper.getAllMacAddresses()

        // Filtrar el mensaje basado en la dirección MAC
        if (registeredMacAddresses.contains(macAddress)) {
            Log.d("Mqtt", "Mac Found")
            // Insertar el valor convertido de data en la base de datos
            val success = dbHelper.insertLog(macAddress, dateTime, dataValue)
            if (success) {
                Log.d("Mqtt", "Data Saved: $dataValue")
                mqttDataListener?.onDataReceived() // Notificar que se han guardado nuevos datos
            } else {
                Log.d("Mqtt", "Data Error: $dataValue")
            }
        }
    }

    private fun convertBsEuiToMac(bsEui: Long): String {
        return String.format(
            "%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X",
            (bsEui shr 56) and 0xFF,
            (bsEui shr 48) and 0xFF,
            (bsEui shr 40) and 0xFF,
            (bsEui shr 32) and 0xFF,
            (bsEui shr 24) and 0xFF,
            (bsEui shr 16) and 0xFF,
            (bsEui shr 8) and 0xFF,
            bsEui and 0xFF
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertNanoTimeToDate(nanoTime: Long): String {
        val seconds = nanoTime / 1_000_000_000
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    private fun convertAsciiArrayToNumber(dataArray: JSONArray): Int {
        val stringBuilder = StringBuilder()
        for (i in 0 until dataArray.length()) {
            val asciiValue = dataArray.getInt(i)
            stringBuilder.append(asciiValue.toChar())
        }
        return stringBuilder.toString().toInt()
    }

    interface MqttDataListener {
        fun onDataReceived()
    }
}

object MqttClientUtil {
    fun getSocketFactory(): javax.net.ssl.SSLSocketFactory {
        return try {
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(MqttX509TrustManager()), java.security.SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private class MqttX509TrustManager : javax.net.ssl.X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
    }
}
