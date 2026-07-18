package com.snehil.auracode.core.di

import com.snehil.auracode.data.repository.AuthRepositoryImpl
import com.snehil.auracode.data.repository.BillingRepositoryImpl
import com.snehil.auracode.data.repository.ChatRepositoryImpl
import com.snehil.auracode.data.repository.FileRepositoryImpl
import com.snehil.auracode.data.repository.ProjectRepositoryImpl
import com.snehil.auracode.domain.repository.AuthRepository
import com.snehil.auracode.domain.repository.BillingRepository
import com.snehil.auracode.domain.repository.ChatRepository
import com.snehil.auracode.domain.repository.FileRepository
import com.snehil.auracode.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository
}
