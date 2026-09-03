package com.swipeclean.app.di

import com.swipeclean.app.data.deletion.MediaDeleterImpl
import com.swipeclean.app.domain.deletion.MediaDeleter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeletionModule {

    @Binds
    @Singleton
    abstract fun bindMediaDeleter(impl: MediaDeleterImpl): MediaDeleter
}
