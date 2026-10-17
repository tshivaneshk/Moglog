package com.skech.moglog.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.skech.moglog.BuildConfig
import com.skech.moglog.data.auth.GoogleAuthClient
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.local.DramaDatabase
import com.skech.moglog.data.local.SettingsStore
import com.skech.moglog.data.remote.TmdbApi
import com.skech.moglog.data.remote.TmdbAuthInterceptor
import com.skech.moglog.data.repository.CloudSyncRepository
import com.skech.moglog.data.repository.DramaRepositoryImpl
import com.skech.moglog.data.repository.ShortcutRepositoryImpl
import com.skech.moglog.data.security.SecureStore
import com.skech.moglog.domain.repository.DramaRepository
import com.skech.moglog.domain.repository.ShortcutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    val MIGRATION_1_5 = object : Migration(1, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `shortcuts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `data` TEXT NOT NULL, `type` TEXT NOT NULL, `iconUrl` TEXT)"
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE dramas ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN completedAt INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE dramas ADD COLUMN myRating REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE dramas ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE dramas ADD COLUMN genres TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE dramas ADD COLUMN releaseYear INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN totalSeasons INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN currentSeason INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE dramas ADD COLUMN currentEpisode INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN nextEpisodeAirDate TEXT")
            database.execSQL("ALTER TABLE dramas ADD COLUMN nextEpisodeNumber INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN nextSeasonNumber INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE dramas ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")

            database.execSQL("ALTER TABLE shortcuts ADD COLUMN dramaId INTEGER NOT NULL DEFAULT 0")

            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `drama_details_cache` (`dramaId` INTEGER NOT NULL, `payloadJson` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`dramaId`))"
            )
        }
    }

    @Provides
    @Singleton
    fun provideSecureStore(app: Application): SecureStore {
        return SecureStore(app)
    }

    @Provides
    @Singleton
    fun provideSettingsStore(app: Application): SettingsStore {
        return SettingsStore(app)
    }

    @Provides
    @Singleton
    fun provideApiKeyManager(
        app: Application,
        secureStore: SecureStore,
        settingsStore: SettingsStore
    ): ApiKeyManager {
        return ApiKeyManager(app, secureStore, settingsStore)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: TmdbAuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                val redacted = message.replace(
                    Regex("api_key=[^&\\s]+"),
                    "api_key=REDACTED"
                )
                android.util.Log.d("MoglogHttp", redacted)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(okHttpClient: OkHttpClient): TmdbApi {
        return Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDramaDatabase(
        app: Application,
        secureStore: SecureStore
    ): DramaDatabase {
        val encryptedFactory = com.skech.moglog.data.security.SqlCipherMigrationHelper
            .createEncryptedOpenHelperFactory(app, "drama_db", secureStore)
        return Room.databaseBuilder(
            app,
            DramaDatabase::class.java,
            "drama_db"
        )
            .openHelperFactory(encryptedFactory)
            .addMigrations(MIGRATION_1_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    @Provides
    @Singleton
    fun provideDramaRepository(
        api: TmdbApi,
        db: DramaDatabase,
        apiKeyManager: ApiKeyManager
    ): DramaRepository {
        return DramaRepositoryImpl(api, db.dao, db.detailCacheDao, apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideShortcutRepository(
        db: DramaDatabase,
        apiKeyManager: ApiKeyManager
    ): ShortcutRepository {
        return ShortcutRepositoryImpl(db.shortcutDao, apiKeyManager)
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
        apiKeyManager: ApiKeyManager,
        secureStore: SecureStore,
        settingsStore: SettingsStore
    ): CloudSyncRepository {
        return CloudSyncRepository(firestore, auth, apiKeyManager, secureStore, settingsStore)
    }
}
