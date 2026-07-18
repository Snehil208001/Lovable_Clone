package com.snehil.auracode.data.network

import com.snehil.auracode.core.common.Constants
import com.snehil.auracode.data.local.TokenDataStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val authEventBus: AuthEventBus
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val request = if (Constants.PUBLIC_PATHS.none { path.endsWith(it) }) {
            val token = tokenDataStore.currentToken()
            if (token.isNullOrBlank()) {
                original
            } else {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
        } else {
            original
        }

        val response = chain.proceed(request)
        if (response.code == 401 && Constants.PUBLIC_PATHS.none { path.endsWith(it) }) {
            authEventBus.notifyUnauthorized()
        }
        return response
    }
}
