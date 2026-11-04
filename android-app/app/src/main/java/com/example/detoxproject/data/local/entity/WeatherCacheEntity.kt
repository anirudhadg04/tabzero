package com.example.detoxproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val locationKey: String = "default",
    val locationName: String = "Bengaluru",
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val precipitationProbability: Int,
    val conditionText: String,
    val isCached: Boolean = true,
    val fetchedAt: Long = System.currentTimeMillis()
)
