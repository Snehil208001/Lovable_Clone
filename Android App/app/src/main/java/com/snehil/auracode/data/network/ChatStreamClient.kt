package com.snehil.auracode.data.network

import com.snehil.auracode.BuildConfig
import com.snehil.auracode.data.local.TokenDataStore
import com.snehil.auracode.data.remote.dto.ChatRequest
import com.snehil.auracode.data.remote.dto.FileReadyPayload
import com.snehil.auracode.domain.model.StreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatStreamClient @Inject constructor(
    okHttpClient: OkHttpClient,
    private val json: Json,
    private val tokenDataStore: TokenDataStore
) {
    // Streaming needs no read timeout.
    private val streamClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun stream(projectId: Long, message: String): Flow<StreamEvent> = callbackFlow {
        val bodyJson = json.encodeToString(ChatRequest.serializer(), ChatRequest(message, projectId))
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/api/chat/stream")
            .addHeader("Accept", "text/event-stream")
            .apply {
                tokenDataStore.currentToken()?.let { addHeader("Authorization", "Bearer $it") }
            }
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                when (type) {
                    "file_ready" -> {
                        val path = runCatching {
                            json.decodeFromString(FileReadyPayload.serializer(), data).path
                        }.getOrNull()
                        if (!path.isNullOrBlank()) trySend(StreamEvent.FileReady(path))
                    }

                    "error" -> {
                        val msg = decodeText(data)
                        trySend(StreamEvent.Failure(msg.ifBlank { "Generation failed" }))
                    }

                    else -> {
                        // Default "chunk" (or unnamed) text token.
                        val text = decodeText(data)
                        if (text.isNotEmpty()) trySend(StreamEvent.Chunk(text))
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(StreamEvent.Completed)
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val message = when {
                    response?.code == 401 -> "Session expired. Please log in again."
                    t != null -> t.message ?: "Streaming failed"
                    else -> "Streaming failed"
                }
                trySend(StreamEvent.Failure(message))
                close()
            }
        }

        val eventSource = EventSources.createFactory(streamClient).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    // Chunks arrive as JSON-encoded strings; fall back to the raw payload.
    private fun decodeText(data: String): String =
        runCatching { json.decodeFromString(String.serializer(), data) }.getOrElse { data }
}
