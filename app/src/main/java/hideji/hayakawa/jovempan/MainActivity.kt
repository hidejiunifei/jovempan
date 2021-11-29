package hideji.hayakawa.jovempan

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

var alarmManager: AlarmManager? = null
lateinit var pendingIntent: PendingIntent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) = MainActivity.callApi(context)
}

class MainActivity : AppCompatActivity() {

    companion object{

        lateinit var sharedPref: SharedPreferences

        fun callApi(context: Context){
            Thread {
                val calendar = Calendar.getInstance()
                val promosURL = URL("https://server.mobradio.com.br/brokers/getGiftsPromos")
                val t : JSONObject

                with(promosURL.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"

                    val postData: ByteArray =
                        StringBuffer("key=4ec46121760ecd5bcc885569bed9042c1b47&date=")
                            .append(calendar.get(Calendar.YEAR))
                            .append("-")
                            .append(calendar.get(Calendar.MONTH)+1)
                            .append("-")
                            .append(calendar.get(Calendar.DAY_OF_MONTH))
                            .append("T")
                            .append(calendar.get(Calendar.HOUR_OF_DAY))
                            .append(":")
                            .append(calendar.get(Calendar.MINUTE)).toString()
                            .toByteArray()

                    val outputStream = DataOutputStream(outputStream)
                    outputStream.write(postData)
                    outputStream.flush()

                    calendar.set(Calendar.MINUTE, 5)
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 1)

                    val inputStream = DataInputStream(inputStream)
                    val reader = BufferedReader(InputStreamReader(inputStream))

                    t = JSONObject(reader.readText())
                }

                val promocoes = t.getJSONArray("promocoes")

                if (promocoes.length() > 0)
                {
                    val data_final = promocoes.getJSONObject(0).getString("data_final")

                    calendar.set(Calendar.HOUR_OF_DAY, data_final.split(' ')[1].split(':')[0].toInt())
                    val premios = promocoes.getJSONObject(0).getJSONArray("premios")

                    val promocaoId = promocoes.getJSONObject(0).getInt("id")
                    val premio1Id = premios.getJSONObject(0).getInt("id")
                    val premio2Id = if (premios.length() > 1) premios.getJSONObject(1).getInt("id") else 0
                    var premioId = 0
                    val premio1Titulo = premios.getJSONObject(0).getString("titulo")
                    val premio2Titulo = if (premios.length() > 1) premios.getJSONObject(1).getString("titulo") else ""
                    val enrollURL = URL("https://server.mobradio.com.br/brokers/promoEnroll")

                    if (premios.length() < 2){
                        premioId = premio1Id
                    } else{
                        for (opcao in sharedPref.getString("csvPrioridadePremios","")!!.split(',')) {
                            if (premio1Titulo.lowercaseChar().contains(opcao)){
                                premioId = premio1Id
                                break
                            }
                            else if (premio2Titulo.lowercaseChar().contains(opcao)) {
                                premioId = premio2Id
                                break
                            }
                        }
                        if (premioId == 0)
                            premioId = premio1Id
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
                        val text = BufferedReader(InputStreamReader(inputStream)).readLines()

                        val builder = NotificationCompat.Builder(context, "CHANNEL_ID")
                            .setContentText(StringBuilder(premioId.toString()).append(text))
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setStyle(NotificationCompat.BigTextStyle().bigText(
                                premios.toString()
                            ))

                        with(NotificationManagerCompat.from(context)){
                            notify(Calendar.getInstance().timeInMillis.toInt(), builder.build())
                        }
                    }
                }
                val builder = NotificationCompat.Builder(context, "CHANNEL_ID")
                    .setContentText(StringBuilder("next run: ").append(calendar.get(Calendar.HOUR_OF_DAY)))
                    .setSmallIcon(R.drawable.ic_launcher_background)

                with(NotificationManagerCompat.from(context)){
                    notify(Calendar.getInstance().timeInMillis.toInt(), builder.build())
                }

                alarmManager?.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        createNotificationChannel()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)

        callApi(this)
    }

    private fun createNotificationChannel(){
        val channel = NotificationChannel("CHANNEL_ID", "CHANNEL_NAME", NotificationManager.IMPORTANCE_DEFAULT)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    fun openSettingsActivity(item: MenuItem?): Boolean {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        return true
    }
}
