package com.nulltheory.treasury

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActionBar
import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
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
    lateinit var layoutContainer: ConstraintLayout
    lateinit var layout: ConstraintLayout
    lateinit var loading: ConstraintLayout
    lateinit var assetsBarrier: Barrier
    lateinit var assets: MutableMap<String, MutableMap<String, TextView>>
    lateinit var prices: MutableMap<String, TextView>
    var recreateContent = false

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
        layoutContainer = findViewById(R.id.layout_content_container)
        layout = findViewById(R.id.layout_content)
        loading = findViewById(R.id.layout_loading)
        prices = mutableMapOf<String, TextView>()
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

    private fun fadeInContent() {
        val duration = resources.getInteger(android.R.integer.config_shortAnimTime)

        layoutContainer.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(duration.toLong())
                .setListener(null)
        }

        loading.animate()
            .alpha(0f)
            .setDuration(duration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    loading.visibility = View.GONE
                }
            })
    }

    private fun clearContent() {
        layoutContainer.visibility = View.GONE
        loading.visibility = View.VISIBLE
        loading.alpha = 1f
        layout.removeAllViewsInLayout()
        assets = mutableMapOf<String, MutableMap<String, TextView>>()
        prices = mutableMapOf<String, TextView>()
        recreateContent = false
    }

    private fun updateText(view: TextView, text: String) {
        val color = if (view.text.isNotEmpty() && view.text != text) {
            Color.WHITE
        } else {
            Color.parseColor("#919191")
        }

        view.text = text
        view.setTextColor(color)
    }

    private fun handleStats(json: JSONObject) {
        val usd = NumberFormat.getCurrencyInstance(Locale.US)
        updateText(textExposure, "%.8f".format(json.optDouble("exposure")))
        updateText(textLeverageDeribit, "%.2f".format(json.optDouble("leverage_deribit")))
        updateText(textLeverageBybit, "%.2f".format(json.optDouble("leverage_bybit")))
        updateText(textCost, usd.format(json.optDouble("cost")))
        updateText(textValue, usd.format(json.optDouble("value")))
        updateText(textPnl, usd.format(json.optDouble("pnl")))
        updateText(textPnlPercentage, "%.2f%%".format(json.optDouble("pnl_percentage")))
    }

    private fun createAssets(json: JSONObject) {
        var previousView: View? = null

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
                previousView?.let { topToBottom = it.id }
                topMargin = (8 * Resources.getSystem().displayMetrics.density).toInt()
            }

            assetBarrier.referencedIds += venueLabel.id

            val venueAssets = json.getJSONObject(venue)

            for (asset in venueAssets.keys()) {
                val quantity = venueAssets.getDouble(asset)
                Log.d("assets", "$venue - $asset - $quantity")

                val assetLabel = TextView(this)
                assetLabel.text = asset
                assetLabel.textSize = 20f
                assetLabel.id = View.generateViewId()
                layout.addView(assetLabel)

                assetLabel.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    previousView?.let { topToBottom = it.id }
                    topMargin = (8 * Resources.getSystem().displayMetrics.density).toInt()
                    startToEnd = assetBarrier.id
                    marginStart = (24 * Resources.getSystem().displayMetrics.density).toInt()
                }

                val assetQuantity = TextView(this)
                assetQuantity.text = if (asset.contains("USD")) {
                    "%,.2f".format(quantity)
                } else {
                    "%.8f".format(quantity)
                }
                assetQuantity.textSize = 20f
                assetQuantity.id = View.generateViewId()
                layout.addView(assetQuantity)

                assetQuantity.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = assetLabel.id
                    endToEnd = layout.id
                }

                assets[venue]!![asset] = assetQuantity
                previousView = assetLabel
            }
        }

        assetsBarrier = Barrier(this)
        assetsBarrier.id = View.generateViewId()
        previousView?.let { assetsBarrier.referencedIds += it.id }
        assetsBarrier.type = Barrier.BOTTOM
        assetsBarrier.margin = (16 * Resources.getSystem().displayMetrics.density).toInt()
        layout.addView(assetsBarrier)
    }

    private fun handleAssets(json: JSONObject) {
        if (assets.isEmpty()) {
            createAssets(json)
            fadeInContent()
            return
        }

        var numberAssets = 0
        var existingAssets = 0
        assets.forEach { existingAssets += it.value.keys.size }

        for (venue in json.keys()) {
            val venueAssets = json.getJSONObject(venue)

            for (asset in venueAssets.keys()) {
                numberAssets++
                val quantity = venueAssets.getDouble(asset)

                if (assets.containsKey(venue) && assets[venue]!!.containsKey(asset)) {
                    val quantityStr = if (asset.contains("USD")) {
                        "%,.2f".format(quantity)
                    } else {
                        "%.8f".format(quantity)
                    }
                    updateText(assets[venue]!![asset]!!, quantityStr)
                } else {
                    recreateContent = true
                }
            }
        }

        if (numberAssets != existingAssets) {
            recreateContent = true
        }
    }

    private fun createPrices(json: JSONObject) {
        var previousView: View = assetsBarrier

        for (symbol in json.keys()) {
            val priceLabel = TextView(this)
            priceLabel.text = symbol
            priceLabel.textSize = 20f
            priceLabel.id = View.generateViewId()
            layout.addView(priceLabel)

            priceLabel.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = previousView.id
                topMargin = (8 * Resources.getSystem().displayMetrics.density).toInt()
            }

            val price = TextView(this)
            price.text = "%,.2f".format(json.getDouble(symbol))
            price.textSize = 20f
            price.id = View.generateViewId()
            layout.addView(price)

            price.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = priceLabel.id
                endToEnd = layout.id
            }

            prices[symbol] = price
            previousView = priceLabel
        }
    }

    private fun handlePrices(json: JSONObject) {
        if (prices.isEmpty()) {
            createPrices(json)
            return
        }

        for (symbol in json.keys()) {
            if (prices.containsKey(symbol)) {
                updateText(prices[symbol]!!, "%,.2f".format(json.getDouble(symbol)))
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
            if (recreateContent) {
                clearContent()
            }
            handleStats(json)
            if (json.has("assets")) {
                handleAssets(json.getJSONObject("assets"))
            }
            if (json.has("prices")) {
                handlePrices(json.getJSONObject("prices"))
            }
        }
    }
}