package com.snehil.auracode.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.snehil.auracode.core.common.Constants
import com.snehil.auracode.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_NAME)

@Singleton
class TokenDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey(Constants.KEY_AUTH_TOKEN)
    private val userIdKey = longPreferencesKey(Constants.KEY_USER_ID)
    private val userNameKey = stringPreferencesKey(Constants.KEY_USER_NAME)
    private val userEmailKey = stringPreferencesKey(Constants.KEY_USER_EMAIL)

    // Cached for synchronous access from the OkHttp interceptor.
    private val cachedToken = AtomicReference<String?>(null)

    init {
        runBlocking { cachedToken.set(context.dataStore.data.first()[tokenKey]) }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    val userFlow: Flow<User?> = context.dataStore.data.map { prefs ->
        val id = prefs[userIdKey] ?: return@map null
        User(
            id = id,
            username = prefs[userEmailKey].orEmpty(),
            name = prefs[userNameKey].orEmpty()
        )
    }

    fun currentToken(): String? = cachedToken.get()

    suspend fun saveSession(token: String, user: User) {
        cachedToken.set(token)
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[userIdKey] = user.id
            prefs[userNameKey] = user.name
            prefs[userEmailKey] = user.username
        }
    }

    suspend fun clear() {
        cachedToken.set(null)
        context.dataStore.edit { it.clear() }
    }
}
