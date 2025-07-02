package br.dev.marconi.lyricalia.repositories.spotify.library

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SpotifyLibraryWebSocket {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(
        baseUrl: String,
        userId: String,
        lifecycleScope: LifecycleCoroutineScope,
        onText: (percentage: String) -> Unit,
        onFinished: (code: Int) -> Unit
    ) {
        val request = Request.Builder()
            .url("$baseUrl/spotify/library")
            .build()

        try {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("IF1001_P3_LYRICALIA", "OPENED Websocket to Spotify Library")
                    send(userId)

                    super.onOpen(webSocket, response)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    lifecycleScope.launch { onText(text) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.d("IF1001_P3_LYRICALIA", "FAILED Websocket to Spotify Library -> ${t.message}")
                    println("Error: ${t.message}")

                    lifecycleScope.launch { onFinished(-1) }
                    super.onFailure(webSocket, t, response)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("IF1001_P3_LYRICALIA", "CLOSING Websocket to Spotify Library -> $code - $reason")
                    lifecycleScope.launch { onFinished(code) }
                    super.onClosing(webSocket, code, reason)
                }
            })
        } catch (ex: Exception) {
            throw Exception("Could not connect to Spotify Library WebSocket progress", ex)
        }
    }

    fun send(message: String) {
        try {
            if (webSocket != null) {
                webSocket!!.send(message)
            } else {
                throw Exception("Socket not connected when trying to send message")
            }
        } catch (ex: Exception) {
            throw Exception("Could not send message to Spotify Library WebSocket", ex)
        }
    }
}