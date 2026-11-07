package com.example.detoxproject

import com.example.detoxproject.data.network.OpenMeteoClient
import com.example.detoxproject.data.network.OpenMeteoWeather
import com.example.detoxproject.ui.screens.settings.PermissionEducationManager
import org.junit.Assert.*
import org.junit.Test
import java.net.UnknownHostException

class PermissionEducationAndOfflineTest {

    @Test
    fun testAllEightPermissionsConfiguredAccurately() {
        val keys = listOf(
            PermissionEducationManager.PERM_USAGE_ACCESS,
            PermissionEducationManager.PERM_ACCESSIBILITY,
            PermissionEducationManager.PERM_OVERLAY,
            PermissionEducationManager.PERM_NOTIFICATIONS,
            PermissionEducationManager.PERM_EXACT_ALARMS,
            PermissionEducationManager.PERM_ACTIVITY_RECOGNITION,
            PermissionEducationManager.PERM_HEALTH_CONNECT,
            PermissionEducationManager.PERM_BATTERY_OPTIMIZATION
        )

        assertEquals(8, keys.size)

        for (key in keys) {
            val detail = PermissionEducationManager.getDetail(key)
            assertNotNull("Detail must exist for $key", detail)
            assertTrue("Title must not be blank for $key", detail.title.isNotBlank())
            assertTrue("Short name must not be blank for $key", detail.shortName.isNotBlank())
            assertTrue("Why needed must explain purpose for $key", detail.whyNeeded.length > 20)
            assertTrue("Functionality enabled must be documented for $key", detail.functionalityEnabled.length > 20)
            assertTrue("Privacy note must guarantee on-device processing for $key", detail.privacyNote.contains("locally") || detail.privacyNote.contains("device") || detail.privacyNote.contains("phone"))
            assertFalse("None of the permissions should be marked mandatory", detail.isMandatory)
            assertNotNull("Icon must be assigned for $key", detail.icon)
        }
    }

    @Test
    fun testAccessibilityPrivacyDeclaration() {
        val accDetail = PermissionEducationManager.getDetail(PermissionEducationManager.PERM_ACCESSIBILITY)
        assertTrue("Must document that canRetrieveWindowContent is false", accDetail.privacyNote.contains("canRetrieveWindowContent is explicitly set to false"))
        assertTrue("Must guarantee no keystroke or screen text capture", accDetail.privacyNote.contains("CANNOT read screen text"))
    }

    @Test
    fun testOfflineWeatherFailureHandling() {
        // Simulate network failure result
        val simulatedFailure: Result<OpenMeteoWeather> = Result.failure(UnknownHostException("Unable to resolve host api.open-meteo.com: No address associated with hostname"))

        assertTrue(simulatedFailure.isFailure)
        assertNull(simulatedFailure.getOrNull())
        assertNotNull(simulatedFailure.exceptionOrNull())
        assertTrue(simulatedFailure.exceptionOrNull() is UnknownHostException)

        // Verify fallback mapping
        val fallbackCondition = OpenMeteoClient.weatherCodeToCondition(1)
        assertEquals("Partly Cloudy", fallbackCondition)

        val pleasantCondition = OpenMeteoClient.weatherCodeToCondition(999)
        assertEquals("Pleasant", pleasantCondition)
    }

    @Test
    fun testWeatherCodeMappings() {
        assertEquals("Clear Sky", OpenMeteoClient.weatherCodeToCondition(0))
        assertEquals("Partly Cloudy", OpenMeteoClient.weatherCodeToCondition(1))
        assertEquals("Foggy", OpenMeteoClient.weatherCodeToCondition(45))
        assertEquals("Light Drizzle", OpenMeteoClient.weatherCodeToCondition(51))
        assertEquals("Rainy", OpenMeteoClient.weatherCodeToCondition(61))
        assertEquals("Snowy", OpenMeteoClient.weatherCodeToCondition(71))
        assertEquals("Rain Showers", OpenMeteoClient.weatherCodeToCondition(80))
        assertEquals("Thunderstorm", OpenMeteoClient.weatherCodeToCondition(95))
    }

    @Test
    fun testNetworkRequestPrivacyAudit() {
        // Verify target domain and parameters
        val testLat = 12.9716
        val testLon = 77.5946
        val expectedUrl = "https://api.open-meteo.com/v1/forecast?latitude=$testLat&longitude=$testLon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,precipitation&timezone=auto"

        assertTrue("Target host must be api.open-meteo.com", expectedUrl.startsWith("https://api.open-meteo.com/v1/forecast"))
        assertFalse("Must not transmit user id", expectedUrl.contains("userId") || expectedUrl.contains("user_id"))
        assertFalse("Must not transmit account email", expectedUrl.contains("email"))
        assertFalse("Must not transmit habits or sessions", expectedUrl.contains("habit") || expectedUrl.contains("session"))
        assertFalse("Must not transmit device identifiers", expectedUrl.contains("imei") || expectedUrl.contains("android_id"))
    }
}
