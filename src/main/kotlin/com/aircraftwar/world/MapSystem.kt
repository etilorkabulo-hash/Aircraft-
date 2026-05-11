package com.aircraftwar.world

import com.aircraftwar.utils.GameLogger
import java.awt.Color
import java.awt.Graphics2D
import kotlin.random.Random

/**
 * Type de terrain avec propriétés visuelles
 */
enum class TerrainType(
    val symbol: String,
    val baseColor: Color,
    val difficulty: Float
) {
    PLAIN("🌾 Plaine", Color(100, 150, 80), 1.0f),
    MOUNTAIN("⛰️ Montagne", Color(150, 100, 80), 1.3f),
    DESERT("🏜️ Désert", Color(200, 180, 100), 1.1f),
    OCEAN("🌊 Océan", Color(50, 100, 200), 1.5f),
    FOREST("🌲 Forêt", Color(50, 100, 50), 1.2f),
    VOLCANIC("🌋 Volcanique", Color(150, 50, 50), 1.4f)
}

/**
 * Système de map dynamique avec transition temporelle
 */
class MapSystem(
    val screenWidth: Int = 800,
    val screenHeight: Int = 600,
    private val updateInterval: Int = 180
) {
    
    private var updateCounter = 0
    private var currentTerrainIndex = 0
    
    var currentTerrain = TerrainType.PLAIN
        private set
    
    var timeOfDay = TimeOfDay.DAY
        private set
    
    // Parallax layers
    private val parallaxLayers = mutableListOf<ParallaxLayer>()
    
    // Particules météo
    private val weatherParticles = mutableListOf<WeatherParticle>()
    
    init {
        initializeParallaxLayers()
        generateWeatherParticles()
        GameLogger.info("🗺️ Système de Map initialisé")
    }
    
    private fun initializeParallaxLayers() {
        // Créer 3 couches de parallax avec différentes vitesses
        repeat(3) { index ->
            val speed = (index + 1) * 0.3f
            val layer = ParallaxLayer(screenWidth, screenHeight, speed)
            parallaxLayers.add(layer)
        }
    }
    
    private fun generateWeatherParticles() {
        weatherParticles.clear()
        repeat(150) {
            weatherParticles.add(WeatherParticle(
                Random.nextDouble(0.0, screenWidth.toDouble()),
                Random.nextDouble(0.0, screenHeight.toDouble())
            ))
        }
    }
    
    fun update(weatherSystem: WeatherSystem) {
        updateCounter++
        
        // Mettre à jour la couche de parallax
        parallaxLayers.forEach { it.update() }
        
        // Mettre à jour les particules météo
        weatherParticles.forEach { 
            it.update(weatherSystem.windForce, weatherSystem.rainIntensity)
        }
        
        // Changer de terrain toutes les X frames
        if (updateCounter >= updateInterval) {
            changeTerrainRandomly()
            updateTimeOfDay()
            updateCounter = 0
        }
    }
    
    private fun changeTerrainRandomly() {
        val newIndex = Random.nextInt(TerrainType.values().size)
        if (newIndex != currentTerrainIndex) {
            currentTerrainIndex = newIndex
            currentTerrain = TerrainType.values()[newIndex]
            GameLogger.info("🗺️ Changement de terrain: ${currentTerrain.symbol}")
            regenerateParallax()
        }
    }
    
    private fun updateTimeOfDay() {
        val newTimeOfDay = listOf(
            TimeOfDay.DAY,
            TimeOfDay.SUNSET,
            TimeOfDay.NIGHT,
            TimeOfDay.SUNRISE
        ).random()
        
        if (newTimeOfDay != timeOfDay) {
            timeOfDay = newTimeOfDay
            GameLogger.info("🕐 Heure: ${timeOfDay.symbol}")
        }
    }
    
    private fun regenerateParallax() {
        parallaxLayers.forEach { it.regenerate(currentTerrain) }
    }
    
    fun render(g: Graphics2D, weatherSystem: WeatherSystem) {
        // Fond de ciel selon l'heure
        val skyColor = timeOfDay.getSkyColor(weatherSystem.currentWeather)
        g.color = skyColor
        g.fillRect(0, 0, screenWidth, screenHeight)
        
        // Rendu des couches de parallax
        parallaxLayers.forEach { it.render(g) }
        
        // Rendu des particules météo
        weatherParticles.forEach { it.render(g, weatherSystem) }
        
        // Overlay de visibilité
        if (weatherSystem.visibility < 1.0f) {
            g.color = Color(200, 200, 200, (100 * (1 - weatherSystem.visibility)).toInt())
            g.fillRect(0, 0, screenWidth, screenHeight)
        }
    }
    
    fun reset() {
        updateCounter = 0
        currentTerrainIndex = 0
        currentTerrain = TerrainType.PLAIN
        timeOfDay = TimeOfDay.DAY
        parallaxLayers.forEach { it.reset() }
    }
}

/**
 * Heure du jour avec effets visuels
 */
enum class TimeOfDay(val symbol: String) {
    DAY("☀️ Jour"),
    SUNSET("🌅 Coucher de soleil"),
    NIGHT("🌙 Nuit"),
    SUNRISE("🌄 Lever de soleil");
    
    fun getSkyColor(weatherType: WeatherType): Color {
        val (r, g, b) = weatherType.color
        return when (this) {
            DAY -> Color(100, 180, 255)
            SUNSET -> Color(255, 100, 50)
            NIGHT -> Color(20, 20, 50)
            SUNRISE -> Color(255, 150, 100)
        }
    }
}

/**
 * Couche de parallax pour effet de profondeur
 */
class ParallaxLayer(
    private val width: Int,
    private val height: Int,
    private val speed: Float
) {
    
    private var offset = 0.0f
    private var terrain = TerrainType.PLAIN
    
    fun update() {
        offset += speed
        if (offset > width) offset = 0f
    }
    
    fun regenerate(newTerrain: TerrainType) {
        terrain = newTerrain
    }
    
    fun render(g: Graphics2D) {
        g.color = Color(
            (terrain.baseColor.red * (1 - speed / 3)).toInt(),
            (terrain.baseColor.green * (1 - speed / 3)).toInt(),
            (terrain.baseColor.blue * (1 - speed / 3)).toInt()
        )
        
        // Dessiner les éléments de parallax
        var x = -offset.toInt()
        while (x < width) {
            g.drawString(terrain.symbol, x, height - 20)
            x += 60
        }
    }
    
    fun reset() {
        offset = 0f
    }
}

/**
 * Particule de météo (pluie, neige, etc.)
 */
class WeatherParticle(
    var x: Double,
    var y: Double
) {
    private var vx = Random.nextDouble(-0.5, 0.5)
    private var vy = Random.nextDouble(1.0, 3.0)
    
    fun update(windForce: Float, rainIntensity: Float) {
        x += vx + windForce
        y += vy * (0.5f + rainIntensity)
        
        // Réapparition au sommet
        if (y > 600) y = -10.0
        if (x > 800) x = -10.0
        if (x < 0) x = 810.0
    }
    
    fun render(g: Graphics2D, weatherSystem: WeatherSystem) {
        when (weatherSystem.currentWeather) {
            WeatherType.RAIN, WeatherType.STORM -> {
                g.color = Color(100, 150, 200, 150)
                g.drawLine(x.toInt(), y.toInt(), (x + vx * 5).toInt(), (y + vy * 5).toInt())
            }
            WeatherType.SNOW -> {
                g.color = Color(220, 220, 255, 180)
                g.fillOval((x - 2).toInt(), (y - 2).toInt(), 4, 4)
            }
            else -> {}
        }
    }
}
