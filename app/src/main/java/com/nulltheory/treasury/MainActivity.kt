package com.nulltheory.treasury

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class MainActivity : AppCompatActivity() {
    lateinit var client: WebSocketClient
    val wsUrl: String = BuildConfig.wsUrl
    val authToken: String = BuildConfig.authToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        client = object : WebSocketClient(URI(wsUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("socket", "onOpen")

                val msg = JSONObject().apply {
                    put("auth", authToken)
                }
                this.send(msg.toString())
            }

            override fun onMessage(message: String?) {
                Log.d("socket", "onMessage: $message")

                val json = JSONObject(message)
                if (json.has("error")) {
                    Log.d("socket", "Authentication failed")
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("socket", "onClose")
            }

            override fun onError(ex: Exception?) {
                Log.d("socket", "onError: ${ex?.message}")
            }

        }

        client.connect()
    }

    override fun onPause() {
        super.onPause()
        client.close()
    }
}