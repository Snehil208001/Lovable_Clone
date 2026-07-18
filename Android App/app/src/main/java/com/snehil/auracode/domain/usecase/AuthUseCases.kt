package com.snehil.auracode.domain.usecase

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.User
import com.snehil.auracode.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Resource<User> =
        repo.login(email.trim(), password)
}

class SignupUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(name: String, email: String, password: String): Resource<User> =
        repo.signup(name.trim(), email.trim(), password)
}

class GetMeUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(): Resource<User> = repo.refreshMe()
}

class LogoutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.logout()
}

class HasSessionUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): Boolean = repo.hasToken()
}
