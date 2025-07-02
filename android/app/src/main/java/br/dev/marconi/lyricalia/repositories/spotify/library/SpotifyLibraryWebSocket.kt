package br.dev.marconi.lyricalia.repositories.spotify.library

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class SpotifyLibraryWebSocket {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(
        baseUrl: String,
        onText: (percentage: String) -> Unit,
        onFinished: (code: Int) -> Unit
    ) {
        val request = Request.Builder()
            .url("$baseUrl/spotify/library")
            .build()

        try {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    Log.d("IF1001_P3_LYRICALIA", "OPENED Websocket to Spotify Library")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onText(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.d("IF1001_P3_LYRICALIA", "FAILED Websocket to Spotify Library -> ${t.message}")
                    println("Error: ${t.message}")
                    super.onFailure(webSocket, t, response)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("IF1001_P3_LYRICALIA", "CLOSED Websocket to Spotify Library -> $code - $reason")
                    println("Closed: $reason")
                    onFinished(code)
                    super.onClosed(webSocket, code, reason)
                }
            })
        } catch (ex: Exception) {
            throw Exception("Could not connect to Spotify Library WebSocket progress", ex)
        }
    }

    fun send(message: String) {
        try {
            webSocket?.send(message)
        } catch (ex: Exception) {
            throw Exception("Could not send message to Spotify Library WebSocket", ex)
        }
    }
}