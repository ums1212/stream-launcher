package org.comon.streamlauncher.domain.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.comon.streamlauncher.domain.model.AppEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryTest {

    private val fakeApps = listOf(
        AppEntity(
            packageName = "com.example.one",
            label = "App One",
            activityName = "com.example.one.MainActivity"
        ),
        AppEntity(
            packageName = "com.example.two",
            label = "App Two",
            activityName = "com.example.two.MainActivity"
        )
    )

    private val repository: AppRepository = FakeAppRepository(fakeApps)

    @Test
    fun `getInstalledApps emits list of AppEntity via Flow`() = runTest {
        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            awaitComplete()
        }
    }

    @Test
    fun `getInstalledApps emits correct AppEntity data`() = runTest {
        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals("com.example.one", items[0].packageName)
            assertEquals("App One", items[0].label)
            assertEquals("com.example.two", items[1].packageName)
            assertEquals("App Two", items[1].label)
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

private class FakeAppRepository(private val apps: List<AppEntity>) : AppRepository {
    override fun getInstalledApps(): Flow<List<AppEntity>> = flowOf(apps)
}
