package com.familyguard.app.di

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.familyguard.app.data.local.dao.*
import com.familyguard.app.data.local.db.FamilyGuardDatabase
import com.familyguard.app.data.remote.api.SyncApi
import com.familyguard.app.security.E2EEncryptionManager
import com.familyguard.app.security.KeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamilyGuardDatabase {
        return Room.databaseBuilder(
            context,
            FamilyGuardDatabase::class.java,
            "familyguard.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideLocationDao(db: FamilyGuardDatabase): LocationDao = db.locationDao()
    @Provides fun provideSafeZoneDao(db: FamilyGuardDatabase): SafeZoneDao = db.safeZoneDao()
    @Provides fun provideSosDao(db: FamilyGuardDatabase): SosDao = db.sosDao()
    @Provides fun provideConsentDao(db: FamilyGuardDatabase): ConsentDao = db.consentDao()
    @Provides fun provideSyncQueueDao(db: FamilyGuardDatabase): SyncQueueDao = db.syncQueueDao()
    @Provides fun provideDeviceProfileDao(db: FamilyGuardDatabase): DeviceProfileDao = db.deviceProfileDao()
    @Provides fun provideFamilyGroupDao(db: FamilyGuardDatabase): FamilyGroupDao = db.familyGroupDao()
    @Provides fun provideAppUsageStatsDao(db: FamilyGuardDatabase): AppUsageStatsDao = db.appUsageStatsDao()
    @Provides fun provideNotificationCountDao(db: FamilyGuardDatabase): NotificationCountDao = db.notificationCountDao()
    @Provides fun provideAnomalyAlertDao(db: FamilyGuardDatabase): AnomalyAlertDao = db.anomalyAlertDao()
    @Provides fun provideUsageBaselineDao(db: FamilyGuardDatabase): UsageBaselineDao = db.usageBaselineDao()
    @Provides fun provideCommunicationMetadataDao(db: FamilyGuardDatabase): CommunicationMetadataDao = db.communicationMetadataDao()
}

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeyManager(@ApplicationContext context: Context): KeyManager {
        return KeyManager(context)
    }

    @Provides
    @Singleton
    fun provideE2EEncryptionManager(keyManager: KeyManager): E2EEncryptionManager {
        return E2EEncryptionManager(keyManager)
    }

    @Provides
    @Singleton
    fun provideAuditLogger(@ApplicationContext context: Context): com.familyguard.app.security.AuditLogger {
        return com.familyguard.app.security.AuditLogger(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        // Use localhost for development, production URL for release
        val baseUrl = if (Build.TYPE == "debug") {
            "http://10.0.2.2:3000/v1/"  // Android emulator localhost
        } else {
            "https://familyguard-api.onrender.com/v1/"
        }
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi {
        return retrofit.create(SyncApi::class.java)
    }
}
