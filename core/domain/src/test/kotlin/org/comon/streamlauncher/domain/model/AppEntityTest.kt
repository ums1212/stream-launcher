package org.comon.streamlauncher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppEntityTest {

    @Test
    fun `AppEntity holds packageName, label, and activityName`() {
        val appEntity = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )

        assertEquals("com.example.app", appEntity.packageName)
        assertEquals("Example App", appEntity.label)
        assertEquals("com.example.app.MainActivity", appEntity.activityName)
    }

    @Test
    fun `AppEntity data class equality is based on all fields`() {
        val appEntity1 = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val appEntity2 = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )

        assertEquals(appEntity1, appEntity2)
    }

    @Test
    fun `AppEntity data class inequality when fields differ`() {
        val appEntity1 = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val appEntity2 = AppEntity(
            packageName = "com.other.app",
            label = "Other App",
            activityName = "com.other.app.MainActivity"
        )

        assertNotEquals(appEntity1, appEntity2)
    }

    @Test
    fun `AppEntity hashCode is equal for equal instances`() {
        val appEntity1 = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val appEntity2 = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )

        assertEquals(appEntity1.hashCode(), appEntity2.hashCode())
    }

    @Test
    fun `AppEntity toString contains all fields`() {
        val appEntity = AppEntity(
            packageName = "com.example.app",
            label = "Example App",
            activityName = "com.example.app.MainActivity"
        )
        val str = appEntity.toString()

        assert(str.contains("com.example.app"))
        assert(str.contains("Example App"))
        assert(str.contains("com.example.app.MainActivity"))
    }
}
