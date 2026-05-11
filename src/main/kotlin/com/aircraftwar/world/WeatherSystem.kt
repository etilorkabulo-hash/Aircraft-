package com.aircraftwar.world

import com.aircraftwar.utils.GameLogger
import kotlin.math.sin
import kotlin.random.Random

/**
 * Système météorologique dynamique
 */
enum class WeatherType(val symbol: String, val color: Triple<Int, Int, Int>) {
    CLEAR("☀️ Ciel Clair", Triple(100, 180, 255)),
    RAIN("🌧️ Pluie", Triple(80, 120, 200)),
    STORM("⛈️ Tempête", Triple(40, 60, 120)),
    FOG("🌫️ Brouillard", Triple(150, 150, 150)),
    SNOW("❄️ Neige", Triple(220, 220, 255))
}

class WeatherSystem(private val cycleDuration: Int = 1200) {
    
    private var timeCounter = 0
    var currentWeather = WeatherType.CLEAR
        private set
    
    // Effets météo
    var visibility = 1.0f // 0.0 à 1.0
        private set
    
    var windForce = 0.0f // -1.0 à 1.0
        private set
    
    var rainIntensity = 0.0f // 0.0 à 1.0
        private set
    
    fun update() {
        timeCounter++
        
        // Cycle météorologique sinus
        val weatherPhase = (timeCounter % cycleDuration).toFloat() / cycleDuration * 360
        val weatherValue = sin(Math.toRadians(weatherPhase.toDouble())).toFloat()
        
        // Déterminer le type de météo
        currentWeather = when {
            weatherValue < -0.6f -> WeatherType.STORM
            weatherValue < -0.2f -> WeatherType.RAIN
            weatherValue < 0.2f -> WeatherType.FOG
            weatherValue < 0.6f -> WeatherType.CLEAR
            else -> WeatherType.SNOW
        }
        
        // Calculer les paramètres météo
        visibility = when (currentWeather) {
            WeatherType.CLEAR -> 1.0f
            WeatherType.RAIN -> 0.85f
            WeatherType.STORM -> 0.6f
            WeatherType.FOG -> 0.4f
            WeatherType.SNOW -> 0.7f
        }
        
        windForce = sin(Math.toRadians(weatherPhase * 2)).toFloat() * when (currentWeather) {
            WeatherType.CLEAR -> 0.1f
            WeatherType.RAIN -> 0.3f
            WeatherType.STORM -> 0.8f
            WeatherType.FOG -> 0.15f
            WeatherType.SNOW -> 0.5f
        }
        
        rainIntensity = when (currentWeather) {
            WeatherType.RAIN -> 0.5f
            WeatherType.STORM -> 1.0f
            else -> 0.0f
        }
    }
    
    fun getWeatherInfo(): String = "${currentWeather.symbol} (Visibilité: ${(visibility * 100).toInt()}%)"
    
    fun reset() {
        timeCounter = 0
        currentWeather = WeatherType.CLEAR
    }
}
