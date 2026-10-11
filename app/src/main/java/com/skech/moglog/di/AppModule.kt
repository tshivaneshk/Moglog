package com.skech.moglog.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skech.moglog.data.local.DramaDatabase
import com.skech.moglog.data.remote.TmdbApi
import com.skech.moglog.data.repository.DramaRepositoryImpl
import com.skech.moglog.domain.repository.DramaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.skech.moglog.data.auth.GoogleAuthClient
import com.skech.moglog.data.repository.CloudSyncRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE dramas ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN completedAt INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideTmdbApi(): TmdbApi {
        return Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDramaDatabase(app: Application): DramaDatabase {
        return Room.databaseBuilder(
            app,
            DramaDatabase::class.java,
            "drama_db"
        ).addMigrations(MIGRATION_5_6).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideApiKeyManager(app: Application): com.skech.moglog.data.local.ApiKeyManager {
        return com.skech.moglog.data.local.ApiKeyManager(app)
    }

    @Provides
    @Singleton
    fun provideDramaRepository(api: TmdbApi, db: DramaDatabase, apiKeyManager: com.skech.moglog.data.local.ApiKeyManager): DramaRepository {
        return DramaRepositoryImpl(api, db.dao, apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideShortcutRepository(db: DramaDatabase): com.skech.moglog.domain.repository.ShortcutRepository {
        return com.skech.moglog.data.repository.ShortcutRepositoryImpl(db.shortcutDao)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideGoogleAuthClient(
        app: Application,
        auth: FirebaseAuth
    ): GoogleAuthClient {
        return GoogleAuthClient(app, auth)
    }

    @Provides
    @Singleton
    fun provideCloudSyncRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        apiKeyManager: com.skech.moglog.data.local.ApiKeyManager
    ): CloudSyncRepository {
        return CloudSyncRepository(firestore, auth, apiKeyManager)
    }
}
