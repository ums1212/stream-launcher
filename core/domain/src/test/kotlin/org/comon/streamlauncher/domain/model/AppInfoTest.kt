package org.comon.streamlauncher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppInfoTest {

    @Test
    fun `AppInfo holds packageName, appName, and activityName`() {
        val appInfo = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )

        assertEquals("com.example.app", appInfo.packageName)
        assertEquals("Example App", appInfo.appName)
        assertEquals("com.example.app.MainActivity", appInfo.activityName)
    }

    @Test
    fun `AppInfo data class equality is based on all fields`() {
        val appInfo1 = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val appInfo2 = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )

        assertEquals(appInfo1, appInfo2)
    }

    @Test
    fun `AppInfo data class inequality when fields differ`() {
        val appInfo1 = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val appInfo2 = AppInfo(
            packageName = "com.other.app",
            appName = "Other App",
            activityName = "com.other.app.MainActivity"
        )

        assertNotEquals(appInfo1, appInfo2)
    }

    @Test
    fun `AppInfo hashCode is equal for equal instances`() {
        val appInfo1 = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val appInfo2 = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )

        assertEquals(appInfo1.hashCode(), appInfo2.hashCode())
    }

    @Test
    fun `AppInfo toString contains all fields`() {
        val appInfo = AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val str = appInfo.toString()

        assert(str.contains("com.example.app"))
        assert(str.contains("Example App"))
        assert(str.contains("com.example.app.MainActivity"))
    }
}
