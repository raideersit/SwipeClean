package com.swipeclean.app.di

import android.content.Context
import androidx.room.Room
import com.swipeclean.app.data.local.AppDatabase
import com.swipeclean.app.data.local.dao.ReviewedMediaDao
import com.swipeclean.app.data.local.dao.SessionStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideReviewedMediaDao(db: AppDatabase): ReviewedMediaDao = db.reviewedMediaDao()

    @Provides
    fun provideSessionStatsDao(db: AppDatabase): SessionStatsDao = db.sessionStatsDao()
}
