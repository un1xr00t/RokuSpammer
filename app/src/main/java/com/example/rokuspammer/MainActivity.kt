package com.example.rokuspammer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.widget.*
import com.example.rokuspammer.R

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private var jobList = mutableListOf<Job>()
    private val keys = listOf("Home", "Up", "Down", "Left", "Right", "Select", "Back", "VolumeUp", "VolumeDown")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ipField = findViewById<EditText>(R.id.ipField)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        startButton.setOnClickListener {
            val ip = ipField.text.toString().trim()
            if (ip.isNotEmpty()) startChaos(ip)
        }

        stopButton.setOnClickListener {
            stopChaos()
        }
    }

    private fun postToRoku(ip: String, path: String) {
        try {
            val req = Request.Builder()
                .url("http://$ip:8060$path")
                .post(ByteArray(0).toRequestBody())  // empty POST body
                .build()

            client.newCall(req).execute().close()
        } catch (e: Exception) {
            e.printStackTrace()
            // Optional: show an error in the app UI
            runOnUiThread {
                Toast.makeText(this, "Failed to send command: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startChaos(ip: String) {
        stopChaos()
        jobList.add(CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = keys.random()
                postToRoku(ip, "/keypress/$key")
                delay(100)
            }
        })
    }

    private fun stopChaos() {
        jobList.forEach { it.cancel() }
        jobList.clear()
    }
}
