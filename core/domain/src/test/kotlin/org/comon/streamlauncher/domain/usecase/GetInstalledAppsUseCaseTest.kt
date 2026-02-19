package org.comon.streamlauncher.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.repository.AppRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GetInstalledAppsUseCaseTest {

    private val fakeApps = listOf(
        AppEntity(packageName = "com.example.one", label = "App One", activityName = "com.example.one.MainActivity"),
        AppEntity(packageName = "com.example.two", label = "App Two", activityName = "com.example.two.MainActivity"),
    )

    private val repository: AppRepository = FakeAppRepository(fakeApps)
    private val useCase = GetInstalledAppsUseCase(repository)

    @Test
    fun `invoke returns flow of installed apps from repository`() = runTest {
        useCase().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("com.example.one", items[0].packageName)
            assertEquals("com.example.two", items[1].packageName)
            awaitComplete()
        }
    }

    @Test
    fun `invoke returns empty flow when repository has no apps`() = runTest {
        val emptyUseCase = GetInstalledAppsUseCase(FakeAppRepository(emptyList()))
        emptyUseCase().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            awaitComplete()
        }
    }
}

private class FakeAppRepository(private val apps: List<AppEntity>) : AppRepository {
    override fun getInstalledApps(): Flow<List<AppEntity>> = flowOf(apps)
}
