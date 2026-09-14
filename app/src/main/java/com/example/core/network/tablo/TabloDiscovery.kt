package com.example.core.network.tablo

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class TabloDiscovery(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "TabloDiscovery"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val assocAdapter = moshi.adapter(TabloAssocResponse::class.java)
    private val serverInfoAdapter = moshi.adapter(TabloServerInfo::class.java)

    /**
     * Executes automatic discovery across:
     * 1. UDP broadcast (Port 8881/8882)
     * 2. Tablo Cloud Association Server (api.tablotv.com/assocserver/getipinfo/)
     * 3. Verifies each discovered IP with GET http://<IP>:8885/server/info
     */
    suspend fun discoverDevices(): List<TabloDiscoveredDevice> = withContext(Dispatchers.IO) {
        val discoveredIps = mutableMapOf<String, String>() // IP -> Method

        coroutineScope {
            val udpDeferred = async {
                try {
                    discoverViaUdp()
                } catch (e: Exception) {
                    Log.w(TAG, "UDP discovery error: ${e.message}")
                    emptyList<Pair<String, String>>()
                }
            }

            val assocDeferred = async {
                try {
                    discoverViaAssocServer()
                } catch (e: Exception) {
                    Log.w(TAG, "Assoc server discovery error: ${e.message}")
                    emptyList<Pair<String, String>>()
                }
            }

            val udpResults = udpDeferred.await()
            val assocResults = assocDeferred.await()

            for ((ip, method) in udpResults) {
                discoveredIps[ip] = method
            }
            for ((ip, method) in assocResults) {
                if (!discoveredIps.containsKey(ip)) {
                    discoveredIps[ip] = method
                }
            }
        }

        // Verify each discovered IP with /server/info
        coroutineScope {
            discoveredIps.map { (ip, method) ->
                async {
                    verifyTabloDevice(ip, method)
                }
            }.awaitAll().filterNotNull()
        }
    }

    /**
     * Method 1: UDP Broadcast on Port 8881, receiving on Port 8882
     */
    private suspend fun discoverViaUdp(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, String>>()
        var receiveSocket: DatagramSocket? = null
        var sendSocket: DatagramSocket? = null
        try {
            // Listen on port 8882 or any available port
            receiveSocket = DatagramSocket(8882).apply {
                soTimeout = 2000
                broadcast = true
            }
            sendSocket = DatagramSocket()

            val broadcastMessage = "tablo-discover".toByteArray()
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val sendPacket = DatagramPacket(broadcastMessage, broadcastMessage.size, broadcastAddr, 8881)
            sendSocket.send(sendPacket)

            val receiveBuffer = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            val endTime = System.currentTimeMillis() + 2000
            while (System.currentTimeMillis() < endTime) {
                try {
                    receiveSocket.receive(receivePacket)
                    val senderIp = receivePacket.address.hostAddress
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    Log.d(TAG, "UDP response from $senderIp: $response")
                    if (senderIp != null && !results.any { it.first == senderIp }) {
                        results.add(senderIp to "UDP Broadcast (8881/8882)")
                    }
                } catch (ste: SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "UDP socket scan: ${e.message}")
        } finally {
            try { sendSocket?.close() } catch (_: Exception) {}
            try { receiveSocket?.close() } catch (_: Exception) {}
        }
        results
    }

    /**
     * Method 2: Tablo Association Server Query
     * https://api.tablotv.com/assocserver/getipinfo/
     */
    private suspend fun discoverViaAssocServer(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, String>>()
        try {
            val request = Request.Builder()
                .url("https://api.tablotv.com/assocserver/getipinfo/")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val assoc = assocAdapter.fromJson(body)
                    assoc?.cpes?.forEach { cpe ->
                        val ip = cpe.privateIp ?: cpe.slip
                        if (!ip.isNullOrBlank()) {
                            results.add(ip to "Tablo Cloud Association Server")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed calling assocserver: ${e.message}")
        }
        results
    }

    /**
     * Verifies that the host at ipAddress:8885 is a Tablo via GET /server/info
     */
    suspend fun verifyTabloDevice(
        ipAddress: String,
        method: String = "Manual IP"
    ): TabloDiscoveredDevice? = withContext(Dispatchers.IO) {
        val cleanIp = ipAddress.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
        val url = "http://$cleanIp:8885/server/info"

        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = withTimeoutOrNull(5000) {
                okHttpClient.newCall(request).execute()
            } ?: return@withContext null

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val info = serverInfoAdapter.fromJson(body) ?: return@withContext null

                TabloDiscoveredDevice(
                    ipAddress = cleanIp,
                    serverId = info.serverId ?: "tablo_$cleanIp",
                    name = info.name ?: "Tablo ($cleanIp)",
                    model = info.model ?: (info.boardType ?: "Tablo DUAL/QUAD"),
                    tunerCount = if (info.tuners > 0) info.tuners else 2,
                    version = info.version ?: "2.2.42",
                    discoveryMethod = method,
                    isAvailable = info.isAvailable
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Verify Tablo at $cleanIp failed: ${e.message}")
            null
        }
    }
}
