package com.rushi.sentinel.di

import android.content.Context
import com.rushi.sentinel.data.db.DatabaseHolder
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
    fun provideDatabaseHolder(
        @ApplicationContext context: Context
    ): DatabaseHolder {
        return DatabaseHolder(context)
    }
}
