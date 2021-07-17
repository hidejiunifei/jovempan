package hideji.hayakawa.jovempan

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

var alarmManager: AlarmManager? = null
var pendingIntent: PendingIntent? = null

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        Thread {
            val promosURL = URL("https://server.mobradio.com.br/brokers/getGiftsPromos")
            val t : JSONObject

            with(promosURL.openConnection() as HttpURLConnection) {
                requestMethod = "POST"

                val postData: ByteArray =
                    StringBuffer("key=4ec46121760ecd5bcc885569bed9042c1b47&date=")
                        .append(calendar.get(Calendar.YEAR))
                        .append("-")
                        .append(calendar.get(Calendar.MONTH))
                        .append("-")
                        .append(calendar.get(Calendar.DAY_OF_MONTH))
                        .append("T")
                        .append(calendar.get(Calendar.HOUR_OF_DAY))
                        .append("T")
                        .append(calendar.get(Calendar.MINUTE)).toString()
                .toByteArray()
                val outputStream = DataOutputStream(outputStream)
                outputStream.write(postData)
                outputStream.flush()

                val inputStream = DataInputStream(inputStream)
                val reader = BufferedReader(InputStreamReader(inputStream))

                t = JSONObject(reader.readText())
            }
			
			val promocoes = t.getJSONArray("promocoes")

			if (promocoes.length() > 0)
			{
				val promocaoId = promocoes.getJSONObject(0).getInt("id")
				val premio1Id = promocoes.getJSONObject(0).getJSONArray("premios").getJSONObject(0).getInt("id")
				val premio2Id = promocoes.getJSONObject(0).getJSONArray("premios").getJSONObject(1).getInt("id")
				val premio1Titulo = promocoes.getJSONObject(0).getJSONArray("premios").getJSONObject(0).getString("titulo")
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

                    var builder = NotificationCompat.Builder(context, "CHANNEL_ID")
                        .setContentText(promocoes.getJSONObject(0).toString())
                        .setSmallIcon(R.drawable.ic_launcher_background)

                    with(NotificationManagerCompat.from(context)){
                        notify(1, builder.build())
                    }
				}
			}
        }.start()

        calendar.set(Calendar.MINUTE, 5)
        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 1)

        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.MINUTE, 5)
        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 1)
        
        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel("CHANNEL_ID", "CHANNEL_NAME", NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}
