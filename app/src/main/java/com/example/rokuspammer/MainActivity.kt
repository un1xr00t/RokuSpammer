package com.example.rokuspammer

import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.*
import java.util.*
import android.view.View


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
        "VolumeUp", "VolumeDown", "Home", "Back", "Select", "PowerOff", "Info", "Up", "Down", "Left", "Right"
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

        // Populate command spinner
        val adapter = ArrayAdapter(this, R.layout.spinner_item, rokuCommands)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        commandSpinner.adapter = adapter


        scanButton.setOnClickListener { scanForRokus() }

        startButton.setOnClickListener { startSpamming() }
        stopButton.setOnClickListener { stopSpamming() }
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
        val foundDevices = mutableListOf<String>()

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
                            foundDevices.add(ip)
                            log("Roku found: $ip")
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
                    deviceSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, foundDevices)
                }
            }
        }
    }

    private fun startSpamming() {
        val targetIp = if (deviceSpinner.selectedItem != null) {
            deviceSpinner.selectedItem.toString()
        } else {
            ipField.text.toString()
        }

        if (targetIp.isBlank()) {
            Toast.makeText(this, "Please enter or select a Roku IP", Toast.LENGTH_SHORT).show()
            return
        }

        val command = commandSpinner.selectedItem.toString()
        val delay = delayInput.text.toString().toLongOrNull() ?: 300L

        spamJob = CoroutineScope(Dispatchers.IO).launch {
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
                        val url = URL("http://$targetIp:8060/keypress/$command")
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