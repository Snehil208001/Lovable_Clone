package com.snehil.auracode.data.repository

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.util.apiCall
import com.snehil.auracode.data.local.TokenDataStore
import com.snehil.auracode.data.remote.AuraCodeApi
import com.snehil.auracode.data.remote.dto.LoginRequest
import com.snehil.auracode.data.remote.dto.SignupRequest
import com.snehil.auracode.data.remote.dto.toDomain
import com.snehil.auracode.domain.model.User
import com.snehil.auracode.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuraCodeApi,
    private val tokenDataStore: TokenDataStore
) : AuthRepository {

    override val token: Flow<String?> = tokenDataStore.tokenFlow
    override val currentUser: Flow<User?> = tokenDataStore.userFlow

    override suspend fun login(email: String, password: String): Resource<User> =
        when (val res = apiCall { api.login(LoginRequest(username = email, password = password)) }) {
            is Resource.Success -> {
                val user = res.data.userProfileResponse.toDomain()
                tokenDataStore.saveSession(res.data.token, user)
                Resource.Success(user)
            }
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun signup(name: String, email: String, password: String): Resource<User> =
        when (val res = apiCall {
            api.signup(SignupRequest(username = email, name = name, password = password))
        }) {
            is Resource.Success -> {
                val user = res.data.userProfileResponse.toDomain()
                tokenDataStore.saveSession(res.data.token, user)
                Resource.Success(user)
            }
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun refreshMe(): Resource<User> =
        when (val res = apiCall { api.me() }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun logout() = tokenDataStore.clear()

    override fun hasToken(): Boolean = !tokenDataStore.currentToken().isNullOrBlank()
}
