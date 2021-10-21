package com.nulltheory.treasury

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var client: WebSocketClient
    lateinit var textExposure: TextView
    lateinit var textLeverageDeribit: TextView
    lateinit var textLeverageBybit: TextView
    lateinit var textCost: TextView
    lateinit var textValue: TextView
    lateinit var textPnl: TextView
    lateinit var textPnlPercentage: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textExposure = findViewById(R.id.textExposure)
        textLeverageDeribit = findViewById(R.id.textLeverageDeribit)
        textLeverageBybit = findViewById(R.id.textLeverageBybit)
        textCost = findViewById(R.id.textCost)
        textValue = findViewById(R.id.textValue)
        textPnl = findViewById(R.id.textPnl)
        textPnlPercentage = findViewById(R.id.textPnlPercentage)
    }

    override fun onResume() {
        super.onResume()
        client = makeClient()
        client.connect()
    }

    override fun onPause() {
        super.onPause()
        client.close()
    }

    private fun makeClient(): WebSocketClient {
        return object : WebSocketClient(URI(BuildConfig.wsUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("socket", "onOpen")

                val msg = JSONObject().apply {
                    put("auth", BuildConfig.authToken)
                }
                this.send(msg.toString())
            }

            override fun onMessage(message: String?) {
                Log.d("socket", "onMessage: $message")
                handlePayload(message ?: "{}")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("socket", "onClose")
            }

            override fun onError(ex: Exception?) {
                Log.d("socket", "onError: ${ex?.message}")
            }
        }
    }

    private fun handlePayload(state: String) {
        val json = JSONObject(state)
        if (json.has("error")) {
            Log.d("socket", "Authentication failed")
            return
        }

        val usd = NumberFormat.getCurrencyInstance(Locale.US)

        runOnUiThread {
            textExposure.text = "%.8f".format(json.optDouble("exposure"))
            textLeverageDeribit.text = "%.2f".format(json.optDouble("leverage_deribit"))
            textLeverageBybit.text = "%.2f".format(json.optDouble("leverage_bybit"))
            textCost.text = usd.format(json.optDouble("cost"))
            textValue.text = usd.format(json.optDouble("value"))
            textPnl.text = usd.format(json.optDouble("pnl"))
            textPnlPercentage.text = "%.2f%%".format(json.optDouble("pnl_percentage"))
        }
    }
}