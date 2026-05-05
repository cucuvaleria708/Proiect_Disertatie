package com.alex.monitorsanatate.data.remote.wifi

import com.alex.monitorsanatate.domain.model.SensorData
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .dispatcher(Dispatcher(Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "okhttp-ws").apply { isDaemon = true }
        }))
        .build()

    private var webSocket: WebSocket? = null

    private val _sensorData = MutableSharedFlow<SensorData>(extraBufferCapacity = 64)
    val sensorData: SharedFlow<SensorData> = _sensorData

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var shouldReconnect = false
    private var currentUrl: String? = null

    fun connect(ip: String, port: Int) {
        val url = "ws://$ip:$port"
        currentUrl = url
        shouldReconnect = true

        // Close any existing socket before creating a new one
        webSocket?.cancel()
        webSocket = null

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseSensorData(text)?.let { data ->
                    _sensorData.tryEmit(data)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                if (shouldReconnect) {
                    scope.launch {
                        delay(3000)
                        currentUrl?.let { url ->
                            val parts = url.removePrefix("ws://").split(":")
                            if (parts.size == 2) {
                                connect(parts[0], parts[1].toIntOrNull() ?: 81)
                            }
                        }
                    }
                }
            }
        })
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _isConnected.value = false
    }

    fun cleanup() {
        shouldReconnect = false
        scope.cancel()
        webSocket?.cancel()
        webSocket = null
        _isConnected.value = false
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    fun sendCommand(command: String) {
        webSocket?.send(command)
    }

    private fun parseSensorData(json: String): SensorData? {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)
            val bpm = obj.get("bpm")?.asInt ?: 0
            val ecgArray = obj.getAsJsonArray("ecg")
            val ecgPoints = mutableListOf<Float>()
            ecgArray?.forEach { element ->
                ecgPoints.add(element.asFloat)
            }
            val timestamp = obj.get("ts")?.asLong ?: System.currentTimeMillis()
            SensorData(bpm = bpm, ecgPoints = ecgPoints, timestamp = timestamp)
        } catch (e: Exception) {
            null
        }
    }
}
