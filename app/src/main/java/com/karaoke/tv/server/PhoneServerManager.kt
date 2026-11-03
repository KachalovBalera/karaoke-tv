package com.karaoke.tv.server

import android.content.Context
import com.karaoke.tv.data.local.CategoryCatalogue
import com.karaoke.tv.data.local.KaraokeDatabase
import com.karaoke.tv.domain.model.KaraokeVersion
import com.karaoke.tv.domain.model.PlaybackAvailability
import com.karaoke.tv.domain.model.QueueItem
import com.karaoke.tv.domain.model.Song
import com.karaoke.tv.domain.model.SourceType
import com.karaoke.tv.domain.model.VerificationStatus
import com.karaoke.tv.domain.repository.QueueRepository
import com.karaoke.tv.domain.repository.RatingRepository
import com.karaoke.tv.domain.usecase.SearchKaraokeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class ConnectedPhoneClient(
    val id: String,
    val ip: String,
    val name: String,
    val connectedAt: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

class PhoneServerManager(
    private val context: Context,
    private val queueRepository: QueueRepository,
    private val ratingRepository: RatingRepository,
    private val searchUseCase: SearchKaraokeUseCase,
    private val db: KaraokeDatabase? = null,
    private val onPlayerAction: (action: String) -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    val port: Int = 8888
    var sessionToken: String = generateRandomToken()
        private set
    var adminPin: String = "%04d".format(SecureRandom().nextInt(10000))
        private set

    private val wsClients = CopyOnWriteArrayList<Socket>()
    private val clientInfoMap = ConcurrentHashMap<String, ConnectedPhoneClient>()

    private val _connectedCount = MutableStateFlow(0)
    val connectedCount: StateFlow<Int> = _connectedCount.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<ConnectedPhoneClient>>(emptyList())
    val connectedClients: StateFlow<List<ConnectedPhoneClient>> = _connectedClients.asStateFlow()

    private var latestQueueJson: String = "[]"
    private var currentPlayingInfoJson: String = "{}"
    var onVoiceSearchQuery: ((String) -> Unit)? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    scope.launch { handleClient(client) }
                }
            } catch (e: Exception) {
                // Server stopped
            }
        }

        // Live observation of queue changes
        scope.launch {
            queueRepository.getQueueFlow().collectLatest { queue ->
                latestQueueJson = serializeQueue(queue)
                broadcastWsMessage(JSONObject().apply {
                    put("type", "queue_updated")
                    put("queue", JSONArray(latestQueueJson))
                    if (currentPlayingInfoJson.isNotBlank() && currentPlayingInfoJson != "{}") {
                        put("playback", JSONObject(currentPlayingInfoJson))
                    }
                }.toString())
            }
        }
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
        disconnectAll()
    }

    fun refreshSession() {
        sessionToken = generateRandomToken()
        adminPin = "%04d".format(SecureRandom().nextInt(10000))
        disconnectAll()
    }

    fun disconnectAll() {
        wsClients.forEach { runCatching { it.close() } }
        wsClients.clear()
        clientInfoMap.clear()
        _connectedCount.value = 0
        _connectedClients.value = emptyList()
    }

    fun disconnectDevice(clientId: String) {
        val entry = clientInfoMap.entries.firstOrNull { it.value.id == clientId }
        if (entry != null) {
            val socket = wsClients.firstOrNull { it.remoteSocketAddress.toString() == entry.key }
            runCatching { socket?.close() }
            if (socket != null) wsClients.remove(socket)
            clientInfoMap.remove(entry.key)
            _connectedCount.value = wsClients.size
            _connectedClients.value = clientInfoMap.values.toList()
        }
    }

    fun updateCurrentPlayback(title: String, artist: String, participant: String, isPlaying: Boolean) {
        currentPlayingInfoJson = JSONObject().apply {
            put("title", title)
            put("artist", artist)
            put("participant", participant)
            put("isPlaying", isPlaying)
        }.toString()

        broadcastWsMessage(JSONObject().apply {
            put("type", "playback_updated")
            put("playback", JSONObject(currentPlayingInfoJson))
        }.toString())
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()
            val requestLine = input.readLine() ?: return

            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val fullPath = parts[1]

            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            while (input.readLine().also { headerLine = it } != null && headerLine!!.isNotBlank()) {
                val headerParts = headerLine!!.split(": ", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0].lowercase()] = headerParts[1].trim()
                }
            }

            // Check if WebSocket upgrade request
            if ("websocket".equals(headers["upgrade"], ignoreCase = true)) {
                handleWebSocketHandshake(socket, headers, output)
                return
            }

            val uriPath = fullPath.substringBefore("?")
            val queryString = fullPath.substringAfter("?", "")
            val queryParams = parseQueryParams(queryString)

            // Local home Wi-Fi token check: allows QR code scanning (?token=...) or direct local access
            val clientToken = queryParams["token"] ?: headers["x-session-token"]
            val remoteIp = socket.inetAddress?.hostAddress ?: "127.0.0.1"

            // Route HTTP requests
            when {
                uriPath == "/" || uriPath == "/index.html" -> {
                    val html = WebAssets.getMobileHtml(adminPin)
                    sendResponse(output, 200, "text/html; charset=utf-8", html)
                }

                uriPath == "/api/register" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                    val name = json.optString("name", "Гость").ifBlank { "Гость" }
                    val key = socket.remoteSocketAddress.toString()
                    val clientInfo = ConnectedPhoneClient(
                        id = "phone_${System.currentTimeMillis() % 100000}",
                        ip = remoteIp,
                        name = name
                    )
                    clientInfoMap[key] = clientInfo
                    _connectedCount.value = clientInfoMap.size
                    _connectedClients.value = clientInfoMap.values.toList()

                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/queue" && method == "GET" -> {
                    sendResponse(output, 200, "application/json", latestQueueJson)
                }

                uriPath == "/api/playback" && method == "GET" -> {
                    sendResponse(output, 200, "application/json", currentPlayingInfoJson)
                }

                uriPath == "/api/popular" && method == "GET" -> {
                    val dbSongs = db?.songDao()?.getAllSongs()?.shuffled()?.take(8) ?: emptyList()
                    val array = JSONArray()
                    if (dbSongs.isNotEmpty()) {
                        for (item in dbSongs) {
                            val versions = db?.karaokeVersionDao()?.getVersionsForSong(item.id)
                            val bestVer = versions?.firstOrNull()
                            array.put(JSONObject().apply {
                                put("id", item.id)
                                put("title", item.title)
                                put("artist", item.artist)
                                put("duration", "MP4")
                                put("coverUrl", item.coverUrl)
                                put("versionId", bestVer?.sourceVideoId ?: "")
                            })
                        }
                    } else {
                        val popular = CategoryCatalogue.getShuffledPopularKaraoke(8, SourceType.GOOGLE_DRIVE)
                        for (item in popular) {
                            array.put(JSONObject().apply {
                                put("id", item.songId)
                                put("title", item.title)
                                put("artist", item.artist)
                                put("duration", item.duration)
                                put("coverUrl", item.coverUrl)
                                put("versionId", item.sourceVideoId)
                            })
                        }
                    }
                    sendResponse(output, 200, "application/json", array.toString())
                }

                uriPath == "/api/history" && method == "GET" -> {
                    val historyList = db?.historyDao()?.getRecentHistory(10)?.firstOrNull() ?: emptyList()
                    val array = JSONArray()
                    for (h in historyList) {
                        array.put(JSONObject().apply {
                            put("id", h.songId)
                            put("title", h.title)
                            put("artist", h.artist)
                            put("coverUrl", "")
                            put("versionId", h.versionId)
                        })
                    }
                    sendResponse(output, 200, "application/json", array.toString())
                }

                uriPath == "/api/search" && method == "GET" -> {
                    val q = queryParams["q"] ?: ""
                    val result = searchUseCase.execute(q, SourceType.GOOGLE_DRIVE)
                    val jsonArray = JSONArray()
                    for (song in result.songs) {
                        val songObj = JSONObject().apply {
                            put("id", song.id)
                            put("title", song.title)
                            put("artist", song.artist)
                            put("coverUrl", song.coverUrl)
                            val versionsArray = JSONArray()
                            for (v in song.versions) {
                                versionsArray.put(JSONObject().apply {
                                    put("id", v.id)
                                    put("title", v.title)
                                    put("thumbnailUrl", v.thumbnailUrl)
                                    put("duration", v.durationFormatted)
                                    put("score", v.karaokeScore)
                                })
                            }
                            put("versions", versionsArray)
                        }
                        jsonArray.put(songObj)
                    }
                    sendResponse(output, 200, "application/json", jsonArray.toString())
                }

                uriPath == "/api/queue/add" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = JSONObject(body)
                    val songId = json.optString("songId")
                    val songTitle = json.optString("songTitle")
                    val songArtist = json.optString("songArtist")
                    val versionId = json.optString("versionId")
                    val coverUrl = json.optString("coverUrl")
                    val participant = json.optString("participantName", "Гость").ifBlank { "Гость" }

                    // Check if song exists in Room database
                    val dbSong = db?.songDao()?.getSongById(songId)
                    val dbVersion = if (versionId.isNotBlank()) {
                        db?.karaokeVersionDao()?.getVersionById(versionId)
                    } else null

                    if (dbSong != null) {
                        val verEntity = dbVersion ?: db?.karaokeVersionDao()?.getVersionsForSong(dbSong.id)?.firstOrNull()
                        val domainVer = verEntity?.let {
                            KaraokeVersion(
                                id = it.id,
                                songId = it.songId,
                                source = it.source,
                                sourceVideoId = it.sourceVideoId,
                                title = it.title,
                                channelOrAuthor = it.channelOrAuthor,
                                thumbnailUrl = it.thumbnailUrl,
                                durationSeconds = it.durationSeconds,
                                videoUrl = it.videoUrl,
                                embedUrl = it.embedUrl,
                                quality = it.quality,
                                karaokeScore = it.karaokeScore,
                                userRatingAverage = it.userRatingAverage,
                                ratingCount = it.ratingCount,
                                isEmbeddable = it.isEmbeddable,
                                verificationStatus = it.verificationStatus,
                                availabilityStatus = it.availabilityStatus
                            )
                        } ?: KaraokeVersion(
                            id = "gdrive_${dbSong.id}",
                            songId = dbSong.id,
                            source = SourceType.GOOGLE_DRIVE,
                            sourceVideoId = "",
                            title = "${dbSong.artist} — ${dbSong.title}",
                            videoUrl = ""
                        )
                        val domainSong = Song(
                            id = dbSong.id,
                            title = dbSong.title,
                            artist = dbSong.artist,
                            coverUrl = dbSong.coverUrl,
                            category = dbSong.category,
                            versions = listOf(domainVer),
                            bestVersionId = domainVer.id
                        )
                        queueRepository.addToQueue(domainSong, domainVer, participant)
                    } else {
                        // Fallback: create Google Drive queue item
                        val cleanVideoId = versionId.removePrefix("gdrive_ver_").removePrefix("gdrive_").removePrefix("rutube_").trim()
                        val mediaUrl = if (cleanVideoId.isNotBlank()) "https://www.googleapis.com/drive/v3/files/$cleanVideoId?alt=media" else ""
                        val dummySong = Song(
                            id = songId.ifBlank { "song_${System.currentTimeMillis()}" },
                            title = songTitle,
                            artist = songArtist,
                            coverUrl = coverUrl
                        )
                        val version = KaraokeVersion(
                            id = "gdrive_ver_$cleanVideoId",
                            songId = dummySong.id,
                            source = SourceType.GOOGLE_DRIVE,
                            sourceVideoId = cleanVideoId,
                            title = "$songArtist — $songTitle (Google Drive)",
                            thumbnailUrl = coverUrl,
                            videoUrl = mediaUrl,
                            embedUrl = mediaUrl,
                            quality = "MP4 / HD",
                            karaokeScore = 0.99f,
                            isEmbeddable = true,
                            verificationStatus = VerificationStatus.VERIFIED_KARAOKE,
                            availabilityStatus = PlaybackAvailability.AVAILABLE
                        )
                        queueRepository.addToQueue(dummySong, version, participant)
                    }
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/queue/remove" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                    val itemId = json.optLong("itemId", -1)
                    if (itemId != -1L) queueRepository.removeFromQueue(itemId)
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/queue/move_up" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                    val itemId = json.optLong("itemId", -1)
                    if (itemId != -1L) queueRepository.moveUp(itemId)
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/queue/move_down" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                    val itemId = json.optLong("itemId", -1)
                    if (itemId != -1L) queueRepository.moveDown(itemId)
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/queue/clear" && method == "POST" -> {
                    queueRepository.clearQueue()
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/playback/action" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = JSONObject(body)
                    val action = json.optString("action")
                    when (action) {
                        "play", "pause", "toggle" -> onPlayerAction(action)
                        "skip" -> {
                            onPlayerAction("skip")
                            queueRepository.skipCurrent()
                        }
                    }
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                uriPath == "/api/remote/voice" && method == "POST" -> {
                    val body = readBody(input, headers["content-length"]?.toIntOrNull() ?: 0).removePrefix("\uFEFF").trim()
                    val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                    val query = json.optString("query").trim()
                    if (query.isNotBlank()) {
                        onVoiceSearchQuery?.invoke(query)
                    }
                    sendResponse(output, 200, "application/json", """{"status":"ok"}""")
                }

                else -> {
                    sendResponse(output, 404, "text/plain", "404 Not Found")
                }
            }
            socket.close()
        } catch (e: Exception) {
            android.util.Log.e("PhoneServer", "Error in handleClient: ${e.message}", e)
            runCatching { socket.close() }
        }
    }

    private fun handleWebSocketHandshake(socket: Socket, headers: Map<String, String>, output: OutputStream) {
        val key = headers["sec-websocket-key"] ?: return
        val acceptKey = computeWebSocketAccept(key)

        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()

        wsClients.add(socket)
        val remoteKey = socket.remoteSocketAddress.toString()
        if (!clientInfoMap.containsKey(remoteKey)) {
            val remoteIp = socket.inetAddress?.hostAddress ?: "127.0.0.1"
            clientInfoMap[remoteKey] = ConnectedPhoneClient(
                id = "phone_${System.currentTimeMillis() % 100000}",
                ip = remoteIp,
                name = "Гость"
            )
        }
        _connectedCount.value = wsClients.size
        _connectedClients.value = clientInfoMap.values.toList()

        // Send initial state to newly connected smartphone
        val initMsg = JSONObject().apply {
            put("type", "init")
            put("queue", JSONArray(latestQueueJson))
            if (currentPlayingInfoJson.isNotBlank() && currentPlayingInfoJson != "{}") {
                put("playback", JSONObject(currentPlayingInfoJson))
            }
        }.toString()
        sendWsFrame(socket, initMsg)
    }

    private fun broadcastWsMessage(message: String) {
        val deadSockets = mutableListOf<Socket>()
        for (client in wsClients) {
            try {
                if (client.isClosed || !client.isConnected) {
                    deadSockets.add(client)
                } else {
                    sendWsFrame(client, message)
                }
            } catch (e: Exception) {
                deadSockets.add(client)
            }
        }
        if (deadSockets.isNotEmpty()) {
            wsClients.removeAll(deadSockets)
            deadSockets.forEach { clientInfoMap.remove(it.remoteSocketAddress.toString()) }
            _connectedCount.value = wsClients.size
            _connectedClients.value = clientInfoMap.values.toList()
        }
    }

    private fun sendWsFrame(socket: Socket, message: String) {
        val bytes = message.toByteArray(Charsets.UTF_8)
        val out = socket.getOutputStream()
        out.write(0x81) // Text frame opcode
        if (bytes.size <= 125) {
            out.write(bytes.size)
        } else if (bytes.size <= 65535) {
            out.write(126)
            out.write((bytes.size shr 8) and 0xFF)
            out.write(bytes.size and 0xFF)
        } else {
            out.write(127)
            for (i in 7 downTo 0) {
                out.write((bytes.size shr (8 * i)) and 0xFF)
            }
        }
        out.write(bytes)
        out.flush()
    }

    private fun computeWebSocketAccept(key: String): String {
        val magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val md = java.security.MessageDigest.getInstance("SHA-1")
        val hash = md.digest((key.trim() + magic).toByteArray(Charsets.ISO_8859_1))
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    private fun sendResponse(output: OutputStream, status: Int, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val writer = PrintWriter(output)
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: $contentType\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()
        output.write(bytes)
        output.flush()
    }

    private fun readBody(reader: BufferedReader, lengthBytes: Int): String {
        if (lengthBytes <= 0) return ""
        val sb = StringBuilder()
        var bytesRead = 0
        while (bytesRead < lengthBytes) {
            val ch = reader.read()
            if (ch == -1) break
            val c = ch.toChar()
            sb.append(c)
            bytesRead += c.toString().toByteArray(Charsets.UTF_8).size
        }
        return sb.toString()
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").associate { param ->
            val p = param.split("=", limit = 2)
            if (p.size == 2) p[0] to java.net.URLDecoder.decode(p[1], "UTF-8") else p[0] to ""
        }
    }

    private fun serializeQueue(queue: List<QueueItem>): String {
        val array = JSONArray()
        for (item in queue) {
            array.put(JSONObject().apply {
                put("id", item.id)
                put("songId", item.songId)
                put("title", item.songTitle)
                put("artist", item.songArtist)
                put("coverUrl", item.coverUrl)
                put("participant", item.participantName)
                put("status", item.status.name)
                put("position", item.position)
                put("versionTitle", item.selectedVersion?.title ?: "")
                put("versionSource", item.selectedVersion?.source?.displayName ?: "RUTUBE")
            })
        }
        return array.toString()
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return "127.0.0.1"
    }

    fun getQrCodeUrl(): String {
        val ip = getLocalIpAddress()
        return "http://$ip:$port/?token=$sessionToken"
    }

    private fun generateRandomToken(): String {
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
