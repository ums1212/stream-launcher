package org.comon.streamlauncher.domain.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.comon.streamlauncher.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryTest {

    private val fakeApps = listOf(
        AppInfo(
            packageName = "com.example.one",
            appName = "App One",
            activityName = "com.example.one.MainActivity"
        ),
        AppInfo(
            packageName = "com.example.two",
            appName = "App Two",
            activityName = "com.example.two.MainActivity"
        )
    )

    private val repository: AppRepository = FakeAppRepository(fakeApps)

    @Test
    fun `getInstalledApps emits list of AppInfo via Flow`() = runTest {
        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            awaitComplete()
        }
    }

    @Test
    fun `getInstalledApps emits correct AppInfo data`() = runTest {
        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals("com.example.one", items[0].packageName)
            assertEquals("App One", items[0].appName)
            assertEquals("com.example.two", items[1].packageName)
            assertEquals("App Two", items[1].appName)
            awaitComplete()
        }
    }

    @Test
    fun `getInstalledApps emits empty list when no apps installed`() = runTest {
        val emptyRepository: AppRepository = FakeAppRepository(emptyList())
        emptyRepository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            awaitComplete()
        }
    }
}

private class FakeAppRepository(private val apps: List<AppInfo>) : AppRepository {
    override fun getInstalledApps(): Flow<List<AppInfo>> = flowOf(apps)
}
