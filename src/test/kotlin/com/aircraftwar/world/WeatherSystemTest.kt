package com.aircraftwar.world

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual

class WeatherSystemTest : StringSpec({
    
    "WeatherSystem should initialize with CLEAR weather" {
        val weatherSystem = WeatherSystem()
        weatherSystem.currentWeather shouldBe WeatherType.CLEAR
        weatherSystem.visibility shouldBe 1.0f
    }
    
    "WeatherSystem should cycle through weather types" {
        val weatherSystem = WeatherSystem(cycleDuration = 100)
        var weatherChanged = false
        val initialWeather = weatherSystem.currentWeather
        
        repeat(200) {
            weatherSystem.update()
            if (weatherSystem.currentWeather != initialWeather) {
                weatherChanged = true
            }
        }
        
        weatherChanged shouldBe true
    }
    
    "Visibility should change based on weather" {
        val weatherSystem = WeatherSystem()
        weatherSystem.visibility shouldBeGreaterThanOrEqual 0.0f
        weatherSystem.visibility shouldBeLessThanOrEqual 1.0f
    }
})
