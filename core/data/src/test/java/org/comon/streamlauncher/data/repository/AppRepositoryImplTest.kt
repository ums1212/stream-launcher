package org.comon.streamlauncher.data.repository

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.comon.streamlauncher.domain.model.AppEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppRepositoryImplTest {

    private lateinit var packageManager: PackageManager
    private lateinit var repository: AppRepositoryImpl

    @Before
    fun setUp() {
        packageManager = mockk()
        repository = AppRepositoryImpl(packageManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * ResolveInfo.activityInfo는 Java public 필드이므로 MockK로 stub 불가.
     * spyk(ResolveInfo())로 spy 생성 후 필드를 직접 할당한다.
     * loadLabel은 메서드이므로 MockK stub 가능.
     */
    private fun createResolveInfo(
        packageName: String,
        activityName: String,
        label: String,
    ): ResolveInfo {
        val activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            this.name = activityName
        }
        val resolveInfo = spyk(ResolveInfo())
        resolveInfo.activityInfo = activityInfo
        every { resolveInfo.loadLabel(packageManager) } returns label
        return resolveInfo
    }

    @Suppress("DEPRECATION")
    private fun stubQueryIntentActivities(result: List<ResolveInfo>) {
        every { packageManager.queryIntentActivities(any<Intent>(), any<Int>()) } returns result
    }

    @Test
    fun `빈 목록 반환 시 빈 리스트를 emit함`() = runTest {
        stubQueryIntentActivities(emptyList())

        repository.getInstalledApps().test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `ResolveInfo를 AppEntity로 올바르게 매핑함`() = runTest {
        val resolveInfo = createResolveInfo(
            packageName = "org.comon.app",
            activityName = "org.comon.app.MainActivity",
            label = "My App",
        )
        stubQueryIntentActivities(listOf(resolveInfo))

        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(
                AppEntity(
                    packageName = "org.comon.app",
                    label = "My App",
                    activityName = "org.comon.app.MainActivity",
                ),
                items[0],
            )
            awaitComplete()
        }
    }

    @Test
    fun `여러 앱을 올바르게 매핑함`() = runTest {
        val apps = listOf(
            createResolveInfo("pkg.a", "pkg.a.Main", "App A"),
            createResolveInfo("pkg.b", "pkg.b.Main", "App B"),
            createResolveInfo("pkg.c", "pkg.c.Main", "App C"),
        )
        stubQueryIntentActivities(apps)

        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("App A", items[0].label)
            assertEquals("App B", items[1].label)
            assertEquals("App C", items[2].label)
            awaitComplete()
        }
    }

    @Test
    fun `Flow가 단일 emission 후 완료됨`() = runTest {
        stubQueryIntentActivities(emptyList())

        repository.getInstalledApps().test {
            awaitItem()
            awaitComplete()
        }
    }

    /**
     * Intent는 Android 스텁 환경에서 내부 상태를 보존하지 않으므로
     * mockkConstructor로 Intent 생성을 가로채고 addCategory 인자를 캡처해 검증한다.
     */
    @Test
    fun `queryIntentActivities에 올바른 Intent를 전달함`() = runTest {
        mockkConstructor(Intent::class)
        val categorySlot = slot<String>()
        every {
            anyConstructed<Intent>().addCategory(capture(categorySlot))
        } returns mockk<Intent>()
        @Suppress("DEPRECATION")
        every { packageManager.queryIntentActivities(any<Intent>(), any<Int>()) } returns emptyList()

        repository.getInstalledApps().test {
            awaitItem()
            awaitComplete()
        }

        assertEquals(Intent.CATEGORY_LAUNCHER, categorySlot.captured)
    }

    @Test
    fun `loadLabel로 앱 이름을 가져옴`() = runTest {
        val resolveInfo = createResolveInfo("pkg.x", "pkg.x.Main", "X App")
        stubQueryIntentActivities(listOf(resolveInfo))

        repository.getInstalledApps().test {
            awaitItem()
            awaitComplete()
        }

        verify { resolveInfo.loadLabel(packageManager) }
    }

    @Test
    fun `activityInfo가 null인 ResolveInfo를 필터링함`() = runTest {
        val nullActivityInfo = spyk(ResolveInfo())
        nullActivityInfo.activityInfo = null

        val validResolveInfo = createResolveInfo("pkg.valid", "pkg.valid.Main", "Valid App")
        stubQueryIntentActivities(listOf(nullActivityInfo, validResolveInfo))

        repository.getInstalledApps().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Valid App", items[0].label)
            awaitComplete()
        }
    }
}
