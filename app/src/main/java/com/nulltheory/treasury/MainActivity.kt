package com.nulltheory.treasury

import android.app.ActionBar
import android.app.Activity
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.marginLeft
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
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
    lateinit var layout: ConstraintLayout
    lateinit var verticalBarrier: Barrier
    lateinit var assets: MutableMap<String, MutableMap<String, TextView>>

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
        assets = mutableMapOf<String, MutableMap<String, TextView>>()
        layout = findViewById(R.id.layout_main_activity)
        verticalBarrier = findViewById<Barrier>(R.id.barrierVertical)
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

    private fun handleStats(json: JSONObject) {
        val usd = NumberFormat.getCurrencyInstance(Locale.US)
        textExposure.text = "%.8f".format(json.optDouble("exposure"))
        textLeverageDeribit.text = "%.2f".format(json.optDouble("leverage_deribit"))
        textLeverageBybit.text = "%.2f".format(json.optDouble("leverage_bybit"))
        textCost.text = usd.format(json.optDouble("cost"))
        textValue.text = usd.format(json.optDouble("value"))
        textPnl.text = usd.format(json.optDouble("pnl"))
        textPnlPercentage.text = "%.2f%%".format(json.optDouble("pnl_percentage"))
    }

    private fun createAssets(json: JSONObject) {
        var previousView: View = findViewById(R.id.barrierStats)

        val assetBarrier = Barrier(this)
        assetBarrier.id = View.generateViewId()
        assetBarrier.type = Barrier.END
        layout.addView(assetBarrier)

        for (venue in json.keys()) {
            Log.d("assets", "Adding venue label - $venue")
            assets[venue] = mutableMapOf<String, TextView>()

            val venueLabel = TextView(this)
            venueLabel.text = venue
            venueLabel.textSize = 20f
            venueLabel.id = View.generateViewId()
            layout.addView(venueLabel)

            venueLabel.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = previousView.id
                topMargin = (8 * Resources.getSystem().displayMetrics.density).toInt()
                startToStart = R.id.textLabelPnl
            }

            assetBarrier.referencedIds += venueLabel.id
            previousView = venueLabel

            val venueAssets = json.getJSONObject(venue)

            for (asset in venueAssets.keys()) {
                val quantity = venueAssets.getDouble(asset)
                Log.d("assets", "$venue - $asset - $quantity")

                val assetLabel = TextView(this)
                assetLabel.text = asset
                assetLabel.textSize = 20f
                assetLabel.id = View.generateViewId()
                layout.addView(assetLabel)

                verticalBarrier.referencedIds += assetLabel.id

                assetLabel.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = previousView.id
                    startToEnd = assetBarrier.id
                    marginStart = (24 * Resources.getSystem().displayMetrics.density).toInt()
                }

                val assetQuantity = TextView(this)
                assetQuantity.text = "%.8f".format(quantity)
                assetQuantity.textSize = 20f
                assetQuantity.id = View.generateViewId()
                layout.addView(assetQuantity)

                assetQuantity.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = assetLabel.id
                    startToEnd = verticalBarrier.id
                    marginStart = (24 * Resources.getSystem().displayMetrics.density).toInt()
                }

                assets[venue]!![asset] = assetQuantity
            }
        }
    }

    private fun handleAssets(json: JSONObject) {
        if (assets.isEmpty()) {
            createAssets(json)
            return
        }

        for (venue in json.keys()) {
            val venueAssets = json.getJSONObject(venue)

            for (asset in venueAssets.keys()) {
                val quantity = venueAssets.getDouble(asset)

                if (assets.containsKey(venue) && assets[venue]!!.containsKey(asset)) {
                    assets[venue]!![asset]?.text = "%.8f".format(quantity)
                }
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
            handleStats(json)
            if (json.has("assets")) {
                handleAssets(json.getJSONObject("assets"))
            }
        }
    }
}