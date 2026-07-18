package com.snehil.auracode.domain.model

sealed interface StreamEvent {
    data class Chunk(val text: String) : StreamEvent
    data class FileReady(val path: String) : StreamEvent
    data class Failure(val message: String) : StreamEvent
    data object Completed : StreamEvent
}
