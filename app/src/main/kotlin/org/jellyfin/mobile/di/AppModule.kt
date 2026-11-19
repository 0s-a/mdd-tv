package org.jellyfin.mobile.di

import androidx.room.Room
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.core.network.AuthInterceptor
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.AppDatabase
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.repository.DownloadsRepositoryImpl
import org.jellyfin.mobile.data.repository.HandshakeRepositoryImpl
import org.jellyfin.mobile.data.repository.HomeRepositoryImpl
import org.jellyfin.mobile.data.repository.LibraryRepositoryImpl
import org.jellyfin.mobile.data.repository.LocalProfileRepositoryImpl
import org.jellyfin.mobile.data.repository.MediaDetailsRepositoryImpl
import org.jellyfin.mobile.data.repository.SearchRepositoryImpl
import org.jellyfin.mobile.domain.repository.DownloadsRepository
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.HomeRepository
import org.jellyfin.mobile.domain.repository.LibraryRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository
import org.jellyfin.mobile.domain.repository.SearchRepository
import org.jellyfin.mobile.domain.usecase.GetHomeFeedUseCase
import org.jellyfin.mobile.domain.usecase.GetLibraryItemsUseCase
import org.jellyfin.mobile.domain.usecase.GetMediaDetailsUseCase
import org.jellyfin.mobile.domain.usecase.SearchMediaUseCase
import org.jellyfin.mobile.presentation.details.MediaDetailsViewModel
import org.jellyfin.mobile.presentation.downloads.DownloadsViewModel
import org.jellyfin.mobile.presentation.home.HomeViewModel
import org.jellyfin.mobile.presentation.library.LibraryViewModel
import org.jellyfin.mobile.presentation.onboarding.OnboardingViewModel
import org.jellyfin.mobile.presentation.player.PlayerViewModel
import org.jellyfin.mobile.presentation.profile.ProfileViewModel
import org.jellyfin.mobile.presentation.search.SearchViewModel
import org.jellyfin.mobile.presentation.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    single { SessionManager() }

    single { AuthInterceptor(sessionManager = get()) }

    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    single {
        val json = get<Json>()
        val contentType = "application/json".toMediaType()
        val rawUrl = AppConfig.SERVER_URL
        val baseUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(get<OkHttpClient>())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single<JellyfinApiService> {
        get<Retrofit>().create(JellyfinApiService::class.java)
    }
}

val repositoryModule = module {
    single<HandshakeRepository> {
        HandshakeRepositoryImpl(
            apiService = get(),
            sessionManager = get()
        )
    }

    single<HomeRepository> {
        HomeRepositoryImpl(
            apiService = get(),
            sessionManager = get(),
            handshakeRepository = get()
        )
    }

    single<LibraryRepository> {
        LibraryRepositoryImpl(
            apiService = get(),
            sessionManager = get(),
            handshakeRepository = get()
        )
    }

    single<MediaDetailsRepository> {
        MediaDetailsRepositoryImpl(
            apiService = get(),
            sessionManager = get(),
            handshakeRepository = get()
        )
    }

    single<SearchRepository> {
        SearchRepositoryImpl(
            apiService = get(),
            sessionManager = get(),
            handshakeRepository = get()
        )
    }

    single<DownloadsRepository> {
        DownloadsRepositoryImpl(
            context = androidContext(),
            dao = get()
        )
    }

    single<LocalProfileRepository> {
        LocalProfileRepositoryImpl(
            userProfileDao = get(),
            localFavoriteDao = get(),
            watchedArchiveDao = get(),
            localWatchProgressDao = get()
        )
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    single { get<AppDatabase>().downloadedMediaDao() }
    single { get<AppDatabase>().userProfileDao() }
    single { get<AppDatabase>().localFavoriteDao() }
    single { get<AppDatabase>().watchedArchiveDao() }
    single { get<AppDatabase>().localWatchProgressDao() }
}

val useCaseModule = module {
    factory { GetHomeFeedUseCase(homeRepository = get()) }
    factory { GetLibraryItemsUseCase(libraryRepository = get()) }
    factory { GetMediaDetailsUseCase(mediaDetailsRepository = get()) }
    factory { SearchMediaUseCase(searchRepository = get()) }
}

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            getHomeFeedUseCase = get(),
            handshakeRepository = get(),
            homeRepository = get(),
            localProfileRepository = get()
        )
    }

    viewModel {
        LibraryViewModel(
            getLibraryItemsUseCase = get(),
            handshakeRepository = get(),
            localProfileRepository = get()
        )
    }

    viewModel {
        MediaDetailsViewModel(
            getMediaDetailsUseCase = get(),
            mediaDetailsRepository = get(),
            handshakeRepository = get(),
            localProfileRepository = get(),
            downloadsRepository = get(),
            context = androidContext()
        )
    }

    viewModel {
        SearchViewModel(
            searchMediaUseCase = get(),
            searchRepository = get(),
            handshakeRepository = get()
        )
    }

    viewModel {
        PlayerViewModel(
            context = androidContext(),
            apiService = get(),
            sessionManager = get(),
            handshakeRepository = get(),
            localProfileRepository = get()
        )
    }

    viewModel {
        DownloadsViewModel(
            downloadsRepository = get(),
            localProfileRepository = get(),
            context = androidContext()
        )
    }

    viewModel {
        OnboardingViewModel(
            localProfileRepository = get()
        )
    }

    viewModel {
        ProfileViewModel(
            localProfileRepository = get()
        )
    }

    viewModel {
        SplashViewModel(
            handshakeRepository = get(),
            localProfileRepository = get()
        )
    }
}

val appModule = module {
    includes(networkModule, databaseModule, repositoryModule, useCaseModule, viewModelModule)
}

val appModules = listOf(networkModule, databaseModule, repositoryModule, useCaseModule, viewModelModule)
