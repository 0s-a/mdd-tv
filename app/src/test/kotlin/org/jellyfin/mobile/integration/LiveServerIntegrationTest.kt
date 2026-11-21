package org.jellyfin.mobile.integration

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.jellyfin.mobile.core.network.AuthInterceptor
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.repository.HandshakeRepositoryImpl
import org.jellyfin.mobile.data.repository.HomeRepositoryImpl
import org.jellyfin.mobile.data.repository.LibraryRepositoryImpl
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Real-world integration test executing against the live Jellyfin server.
 */
class LiveServerIntegrationTest {

    private val serverUrl = "http://192.168.200.200:80/"
    private val testUsername = "الاستراحه"

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: JellyfinApiService
    private lateinit var handshakeRepo: HandshakeRepositoryImpl
    private lateinit var libraryRepo: LibraryRepositoryImpl
    private lateinit var homeRepo: HomeRepositoryImpl

    @Before
    fun setUp() {
        val isReachable = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("192.168.200.200", 80), 1500)
                true
            }
        } catch (e: Exception) {
            false
        }
        assumeTrue("Skipping integration test: Live Jellyfin server at 192.168.200.200:80 is unreachable in this environment.", isReachable)

        sessionManager = SessionManager()
        val authInterceptor = AuthInterceptor(sessionManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        apiService = retrofit.create(JellyfinApiService::class.java)
        handshakeRepo = HandshakeRepositoryImpl(apiService, sessionManager)
        libraryRepo = LibraryRepositoryImpl(apiService, sessionManager, handshakeRepo)
        homeRepo = HomeRepositoryImpl(apiService, sessionManager, handshakeRepo)
    }

    @Test
    fun testLiveHandshakeAndAuthentication() = runBlocking {
        // 1. Handshake
        val sessionResult = handshakeRepo.performHandshake(testUsername, "")
        assertTrue("Handshake should succeed", sessionResult.isSuccess)
        val session = sessionResult.getOrThrow()
        assertEquals(testUsername, session.userName)
        assertTrue("Token must be non-empty", session.token.isNotBlank())
        assertTrue("UserId must be non-empty", session.userId.isNotBlank())

        // 2. Server info
        val serverInfo = handshakeRepo.serverInfo.value
        assertNotNull("ServerInfo should be populated", serverInfo)
        assertTrue("OS should be Windows", serverInfo!!.operatingSystem.contains("Windows", ignoreCase = true))
    }

    @Test
    fun testLiveLibrariesDiscovery() = runBlocking {
        handshakeRepo.performHandshake(testUsername, "")
        val viewsResult = handshakeRepo.getUserViews()
        assertTrue("getUserViews should succeed", viewsResult.isSuccess)
        val views = viewsResult.getOrThrow()
        assertEquals("Server should have exactly 11 libraries", 11, views.size)

        val libraryNames = views.map { it.name }
        assertTrue("Should include 'قسم الافلام'", libraryNames.contains("قسم الافلام"))
        assertTrue("Should include 'الانمي والاطفال'", libraryNames.contains("الانمي والاطفال"))
        assertTrue("Should include 'مسلسلات رمضان'", libraryNames.contains("مسلسلات رمضان"))
    }

    @Test
    fun testLiveHierarchicalBrowsingVersusFlatMode() = runBlocking {
        handshakeRepo.performHandshake(testUsername, "")
        val views = handshakeRepo.getUserViews().getOrThrow()
        val animeLib = views.first { it.name == "الانمي والاطفال" }

        // Hierarchical mode (isHierarchical = true) -> returns top-level folders
        val hierResult = libraryRepo.getLibraryItems(
            parentId = animeLib.id,
            filter = LibraryFilter(),
            letter = null,
            startIndex = 0,
            limit = 20,
            isHierarchical = true
        )
        assertTrue("Hierarchical fetch should succeed", hierResult.isSuccess)
        val hierPage = hierResult.getOrThrow()
        assertTrue("Hierarchical items should be <= 10 top-level items", hierPage.items.size <= 10)
        assertTrue("All top-level items should be folders", hierPage.items.all { it.isFolder })

        // Flat mode (isHierarchical = false) -> returns catalog of leaf items
        val flatResult = libraryRepo.getLibraryItems(
            parentId = animeLib.id,
            filter = LibraryFilter(),
            letter = null,
            startIndex = 0,
            limit = 20,
            isHierarchical = false
        )
        assertTrue("Flat fetch should succeed", flatResult.isSuccess)
        val flatPage = flatResult.getOrThrow()
        assertTrue("Total count in flat mode should be large (>1000 items)", flatPage.totalCount > 1000)
    }

    @Test
    fun testLiveNextUpAndResume() = runBlocking {
        handshakeRepo.performHandshake(testUsername, "")
        val nextUpResult = homeRepo.getNextUpItems(limit = 10)
        assertTrue("getNextUpItems should succeed", nextUpResult.isSuccess)

        val resumeResult = homeRepo.getResumeItems(limit = 10)
        assertTrue("getResumeItems should succeed", resumeResult.isSuccess)
    }
}
