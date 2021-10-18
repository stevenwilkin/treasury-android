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

        runOnUiThread {
            textExposure.text = json.optDouble("exposure").toString()
            textLeverageDeribit.text = json.optDouble("leverage_deribit").toString()
            textLeverageBybit.text = json.optDouble("leverage_bybit").toString()
            textCost.text = json.optDouble("cost").toString()
            textValue.text = json.optDouble("value").toString()
            textPnl.text = json.optDouble("pnl").toString()
            textPnlPercentage.text = json.optDouble("pnl_percentage").toString()
        }
    }
}