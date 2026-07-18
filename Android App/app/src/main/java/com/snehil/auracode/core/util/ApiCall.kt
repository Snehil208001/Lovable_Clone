package com.snehil.auracode.core.util

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.data.remote.dto.ApiErrorDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

suspend fun <T> apiCall(block: suspend () -> T): Resource<T> =
    try {
        Resource.Success(block())
    } catch (e: HttpException) {
        Resource.Error(message = e.toUserMessage(), code = e.code())
    } catch (e: IOException) {
        Resource.Error("Network error. Please check your connection.")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Something went wrong.")
    }

private fun HttpException.toUserMessage(): String {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val parsed = raw?.takeIf { it.isNotBlank() }?.let {
        runCatching { errorJson.decodeFromString(ApiErrorDto.serializer(), it).message }.getOrNull()
    }
    return parsed ?: when (code()) {
        401 -> "Session expired. Please log in again."
        403 -> "You don't have permission to do that."
        404 -> "Not found."
        in 500..599 -> "Server error. Please try again later."
        else -> "Request failed (${code()})."
    }
}
