package com.example.detoxproject.domain.engine

data class WellnessRecommendation(
    val id: String,
    val title: String,
    val category: String,
    val durationMinutes: Int,
    val isOutdoor: Boolean,
    val reason: String
)

object WellnessRecommendationEngine {

    fun generateRecommendations(
        temperature: Double,
        weatherCode: Int,
        precipitation: Double,
        screenTimeMs: Long,
        stepsToday: Int,
        stepsGoal: Int = 8000
    ): List<WellnessRecommendation> {
        val screenTimeHours = screenTimeMs / (1000.0 * 60 * 60)
        val isRainy = precipitation > 0.5 || weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
        val isExcessiveHeat = temperature >= 34.0
        val isHighScreenTime = screenTimeHours >= 2.0
        val isLowSteps = stepsToday < (stepsGoal / 2)

        val list = mutableListOf<WellnessRecommendation>()

        when {
            // Case 1: Rainy or stormy weather -> indoor recommendations
            isRainy -> {
                list.add(
                    WellnessRecommendation(
                        id = "rec_rain_read",
                        title = "Rainy Day Paperback Reading",
                        category = "reading",
                        durationMinutes = 30,
                        isOutdoor = false,
                        reason = "Wet weather outside. Unplug from screens with a physical paperback."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_rain_stretch",
                        title = "Indoor Mobility & Stretch",
                        category = "movement",
                        durationMinutes = 20,
                        isOutdoor = false,
                        reason = if (isHighScreenTime) "Rain keeps you indoors; stretch out prolonged screen-time tension." else "Gentle indoor movement while listening to the rain."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_rain_tea",
                        title = "Mindful Tea Ritual",
                        category = "mindfulness",
                        durationMinutes = 15,
                        isOutdoor = false,
                        reason = "Brew tea and enjoy every sip with your smartphone face down in another room."
                    )
                )
            }

            // Case 2: Excessive heat -> cool indoor focus
            isExcessiveHeat -> {
                list.add(
                    WellnessRecommendation(
                        id = "rec_heat_meditate",
                        title = "Breathwork & Meditation",
                        category = "mindfulness",
                        durationMinutes = 15,
                        isOutdoor = false,
                        reason = "Excessive heat outside ($temperature°C). Cool down indoors with phone screen locked."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_heat_journal",
                        title = "Pen & Paper Journaling",
                        category = "mindfulness",
                        durationMinutes = 25,
                        isOutdoor = false,
                        reason = "Write your reflections in an analog notebook to give tired eyes a rest."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_heat_indoor_core",
                        title = "Gentle Living-Room Yoga",
                        category = "movement",
                        durationMinutes = 20,
                        isOutdoor = false,
                        reason = "Stay active in climate-controlled comfort without screen distractions."
                    )
                )
            }

            // Case 3: Pleasant weather + High Screen Time + Low Steps -> Phone-free outdoor walk
            isHighScreenTime && isLowSteps -> {
                list.add(
                    WellnessRecommendation(
                        id = "rec_outdoor_walk",
                        title = "Phone-Free Neighborhood Walk",
                        category = "movement",
                        durationMinutes = 30,
                        isOutdoor = true,
                        reason = "You have ${"%.1f".format(screenTimeHours)}h screen time and low steps today. Great ${temperature.toInt()}°C weather for an outdoor walk without your phone!"
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_park_read",
                        title = "Park Bench Reading",
                        category = "reading",
                        durationMinutes = 35,
                        isOutdoor = true,
                        reason = "Take a physical book to a shaded outdoor bench and leave phone notifications behind."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_fresh_air",
                        title = "Outdoor Mindful Breathing",
                        category = "mindfulness",
                        durationMinutes = 15,
                        isOutdoor = true,
                        reason = "Reset mental energy and eye strain with 15 minutes of open-sky mindfulness."
                    )
                )
            }

            // Case 4: General pleasant weather
            else -> {
                list.add(
                    WellnessRecommendation(
                        id = "rec_outdoor_stroll",
                        title = "Neighborhood Walk",
                        category = "movement",
                        durationMinutes = 30,
                        isOutdoor = true,
                        reason = "Great weather outside ($temperature°C) for an uninterrupted walk without headphones."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_park_read_gen",
                        title = "Park Bench Reading",
                        category = "reading",
                        durationMinutes = 35,
                        isOutdoor = true,
                        reason = "Take a physical paperback to a quiet shaded park."
                    )
                )
                list.add(
                    WellnessRecommendation(
                        id = "rec_mindful_tea_gen",
                        title = "Mindful Tea Ritual",
                        category = "mindfulness",
                        durationMinutes = 15,
                        isOutdoor = false,
                        reason = "Brew tea and enjoy every sip with your phone face-down."
                    )
                )
            }
        }

        return list
    }
}
