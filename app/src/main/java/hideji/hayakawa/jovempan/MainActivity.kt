package hideji.hayakawa.jovempan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.logging.Logger

var alarmManager: AlarmManager? = null
var pendingIntent: PendingIntent? = null

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Thread {
            val promosURL = URL("https://server.mobradio.com.br/brokers/getGiftsPromos")
            val t : JSONObject

            with(promosURL.openConnection() as HttpURLConnection) {
                requestMethod = "POST"

                val postData: ByteArray =
                    "key=4ec46121760ecd5bcc885569bed9042c1b47&date=2021-07-05T08:01".toByteArray()
                val outputStream = DataOutputStream(outputStream)
                outputStream.write(postData)
                outputStream.flush()

                val inputStream = DataInputStream(inputStream)
                val reader = BufferedReader(InputStreamReader(inputStream))

                t = JSONObject(reader.readText())
            }

            val promocaoId = t.getJSONArray("promocoes").getJSONObject(0).getInt("id")
            val premio1Id = t.getJSONArray("promocoes").getJSONObject(0).getJSONArray("premios").getJSONObject(0).getInt("id")
            val premio2Id = t.getJSONArray("promocoes").getJSONObject(0).getJSONArray("premios").getJSONObject(1).getInt("id")
            val premio1Titulo = t.getJSONArray("promocoes").getJSONObject(0).getJSONArray("premios").getJSONObject(0).getString("titulo")
            val enrollURL = URL("https://server.mobradio.com.br/brokers/promoEnroll")

            val premioId: Int = if (premio1Titulo.contains("pizza", true) ||
                premio1Titulo.contains("sushi", true) ||
                premio1Titulo.contains("mexican", true)){
                premio1Id
            } else{
                premio2Id
            }

            with(enrollURL.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData: ByteArray =
                    "app_key=4ec46121760ecd5bcc885569bed9042c1b47&gift_promo_id=$promocaoId&gift_id=$premioId&listener_id=122304".toByteArray()
                val outputStream = DataOutputStream(outputStream)
                outputStream.write(postData)
                outputStream.flush()

                val inputStream = DataInputStream(inputStream)
                val reader = BufferedReader(InputStreamReader(inputStream))

                Toast.makeText(context, reader.readText(), Toast.LENGTH_SHORT).show()
            }
            //Toast.makeText(context, responseMessage, Toast.LENGTH_LONG).show()
        }.start()

        var calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 1);

        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)

        var calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 1);
        
        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}
