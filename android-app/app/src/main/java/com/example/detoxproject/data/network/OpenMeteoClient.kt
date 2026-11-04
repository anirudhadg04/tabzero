package com.example.detoxproject.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OpenMeteoWeather(
    val locationName: String = "Current Location",
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val conditionText: String,
    val isCached: Boolean = false
)

class OpenMeteoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    companion object {
        fun weatherCodeToCondition(code: Int): String {
            return when (code) {
                0 -> "Clear Sky"
                1, 2, 3 -> "Partly Cloudy"
                45, 48 -> "Foggy"
                51, 53, 55 -> "Light Drizzle"
                61, 63, 65 -> "Rainy"
                71, 73, 75 -> "Snowy"
                80, 81, 82 -> "Rain Showers"
                95, 96, 99 -> "Thunderstorm"
                else -> "Pleasant"
            }
        }
    }

    suspend fun fetchCurrentWeather(
        lat: Double = 12.9716,
        lon: Double = 77.5946
    ): Result<OpenMeteoWeather> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,precipitation&timezone=auto"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "tabzero-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Open-Meteo HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty weather response"))
                val root = JSONObject(body)
                val current = root.optJSONObject("current") ?: return@withContext Result.failure(Exception("Missing 'current' object"))

                val temp = current.optDouble("temperature_2m", 25.0)
                val code = current.optInt("weather_code", 1)
                val humidity = current.optInt("relative_humidity_2m", 50)
                val wind = current.optDouble("wind_speed_10m", 8.0)
                val precip = current.optDouble("precipitation", 0.0)
                val condition = weatherCodeToCondition(code)

                Result.success(
                    OpenMeteoWeather(
                        locationName = "Current Location",
                        temperature = temp,
                        weatherCode = code,
                        humidity = humidity,
                        windSpeed = wind,
                        precipitation = precip,
                        conditionText = condition,
                        isCached = false
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
