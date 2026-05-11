package com.aircraftwar.game

import com.aircraftwar.utils.GameLogger
import com.aircraftwar.world.MapSystem
import com.aircraftwar.world.WeatherSystem
import java.awt.Graphics2D
import kotlin.math.abs
import kotlin.random.Random

/**
 * Gestionnaire principal du jeu avec système de progression
 */
class GameManager(
    private val screenWidth: Int = 800,
    private val screenHeight: Int = 600
) {
    
    // Systèmes du monde
    val mapSystem = MapSystem(screenWidth, screenHeight)
    val weatherSystem = WeatherSystem()
    
    // Entités du jeu
    val player = Player(screenWidth = screenWidth, screenHeight = screenHeight)
    private val enemies = mutableListOf<Enemy>()
    private val projectiles = mutableListOf<Projectile>()
    private val powerUps = mutableListOf<PowerUp>()
    private val explosions = mutableListOf<Explosion>()
    private val comboEffects = mutableListOf<ComboEffect>()
    
    // État du jeu
    var score = 0
    var wave = 1
    var difficulty = 1.0f
    
    private var waveTimer = 0
    private val waveInterval = 350
    private var enemySpawnCount = 0
    private var enemiesPerWave = 5
    
    var isPaused = false
    var isGameOver = false
    
    private val random = Random(System.currentTimeMillis())
    
    // Statistiques
    var totalEnemiesDefeated = 0
    var totalProjectilesFired = 0
    var totalDamageDealt = 0
    var longestCombo = 0
    var currentCombo = 0
    var comboTimer = 0
    
    init {
        GameLogger.info("🎮 Gestionnaire de jeu initialisé (Difficulté: $difficulty)")
    }
    
    fun update() {
        if (isPaused || isGameOver) return
        
        // Mise à jour des systèmes mondiaux
        weatherSystem.update()
        mapSystem.update(weatherSystem)
        
        // Mise à jour du joueur
        player.update(weatherSystem)
        player.damageMultiplier = when (weatherSystem.currentWeather) {
            com.aircraftwar.world.WeatherType.STORM -> 1.2f
            com.aircraftwar.world.WeatherType.RAIN -> 0.9f
            else -> 1.0f
        }
        
        // Mise à jour des ennemis
        enemies.forEach { it.update(weatherSystem) }
        enemies.removeAll { !it.isAlive() || it.isOffScreen() }
        
        // Mise à jour des projectiles
        projectiles.forEach { it.update(weatherSystem) }
        projectiles.removeAll { it.isOffScreen() }
        
        // Mise à jour des power-ups
        powerUps.forEach { it.update(weatherSystem) }
        powerUps.removeAll { it.isOffScreen() }
        
        // Mise à jour des explosions
        explosions.forEach { it.update() }
        explosions.removeAll { !it.isAlive() }
        
        // Mise à jour des effets de combo
        comboEffects.forEach { it.update() }
        comboEffects.removeAll { !it.isAlive() }
        
        // Gestion des vagues
        updateWaves()
        
        // Collisions
        checkCollisions()
        
        // Gestion du combo
        comboTimer--
        if (comboTimer <= 0) {
            currentCombo = 0
        }
        
        // Vérification fin du jeu
        if (!player.isAlive()) {
            isGameOver = true
            GameLogger.info("💀 Fin de partie! Score final: $score")
        }
    }
    
    private fun updateWaves() {
        waveTimer++
        
        if (waveTimer >= waveInterval && enemySpawnCount < enemiesPerWave) {
            spawnEnemy()
            enemySpawnCount++
            waveTimer = 0
        }
        
        if (enemies.isEmpty() && enemySpawnCount >= enemiesPerWave) {
            nextWave()
        }
    }
    
    private fun nextWave() {
        wave++
        difficulty = 1.0f + (wave - 1) * 0.15f
        enemiesPerWave = (5 + wave * 2).coerceAtMost(25)
        enemySpawnCount = 0
        
        // Boss tous les 5 vagues
        if (wave % 5 == 0) {
            spawnBoss()
        }
        
        GameLogger.info("🌊 Vague $wave! (Difficulté: ${String.format("%.2f", difficulty)})")
    }
    
    private fun spawnEnemy() {
        val x = random.nextDouble(50.0, (screenWidth - 50).toDouble())
        
        val enemyType = when {
            wave < 3 -> EnemyType.BASIC
            wave < 7 -> if (random.nextDouble() < 0.6) EnemyType.BASIC else EnemyType.STRONG
            wave < 12 -> listOf(EnemyType.BASIC, EnemyType.STRONG, EnemyType.FAST)
                .random(random)
            else -> listOf(EnemyType.BASIC, EnemyType.STRONG, EnemyType.FAST)
                .random(random)
        }
        
        enemies.add(Enemy(x, -40.0, enemyType, difficulty))
    }
    
    private fun spawnBoss() {
        val x = screenWidth / 2.0 - 20.0
        enemies.add(Enemy(x, -50.0, EnemyType.BOSS, difficulty * 1.5f))
        GameLogger.info("👹 BOSS apparait!")
    }
    
    private fun checkCollisions() {
        // Collision joueur-ennemis
        enemies.forEach { enemy ->
            if (player.collidesWith(enemy)) {
                player.takeDamage(15)
                GameLogger.debug("💥 Collision joueur-ennemi!")
            }
        }
        
        // Collision projectiles-ennemis
        val projectilesToRemove = mutableListOf<Projectile>()
        
        projectiles.forEach { projectile ->
            if (projectile.isPlayerBullet) {
                enemies.forEach { enemy ->
                    if (projectile.collidesWith(enemy)) {
                        val damage = projectile.damage
                        enemy.takeDamage(damage)
                        totalDamageDealt += damage
                        
                        currentCombo++
                        comboTimer = 120 // 2 secondes
                        
                        if (currentCombo > longestCombo) {
                            longestCombo = currentCombo
                        }
                        
                        if (!enemy.isAlive()) {
                            val enemyScore = enemy.getScore()
                            score += (enemyScore * (1 + currentCombo * 0.1)).toInt()
                            totalEnemiesDefeated++
                            
                            player.gainExperience(enemyScore)
                            
                            // Afficher effet combo
                            if (currentCombo > 1) {
                                comboEffects.add(ComboEffect(enemy.x, enemy.y, currentCombo))
                            }
                            
                            // Chance de drop
                            if (random.nextDouble() < 0.25 + (wave * 0.02)) {
                                val powerUpType = PowerUpType.values().random(random)
                                powerUps.add(PowerUp(enemy.x + 15, enemy.y + 15, powerUpType))
                            }
                            
                            explosions.add(Explosion(enemy.x + 20, enemy.y + 20, 30))
                        }
                        
                        projectilesToRemove.add(projectile)
                    }
                }
            }
        }
        
        projectiles.removeAll(projectilesToRemove)
        
        // Collision joueur-power-ups
        powerUps.forEach { powerUp ->
            if (player.collidesWith(powerUp)) {
                powerUp.type.effect(player)
                powerUps.remove(powerUp)
                score += powerUp.type.rarityScore
            }
        }
    }
    
    fun fireProjectile() {
        if (player.ammo > 0 && !isPaused) {
            val projectile = Projectile(
                player.x + player.width / 2 - 3,
                player.y - 10,
                true,
                player.damageMultiplier
            )
            projectiles.add(projectile)
            player.ammo--
            totalProjectilesFired++
        }
    }
    
    fun fireEnemyProjectile(enemy: Enemy) {
        projectiles.add(Projectile(
            enemy.x + enemy.width / 2 - 3,
            enemy.y + enemy.height,
            false,
            1.0f
        ))
    }
    
    fun enemyShoot() {
        enemies.filter { it.shouldShoot() }.forEach { fireEnemyProjectile(it) }
    }
    
    fun render(g: Graphics2D) {
        // Rendu de la map et météo
        mapSystem.render(g, weatherSystem)
        
        // Rendu du joueur
        player.render(g)
        
        // Rendu des ennemis
        enemies.forEach { it.render(g) }
        
        // Rendu des projectiles
        projectiles.forEach { it.render(g) }
        
        // Rendu des power-ups
        powerUps.forEach { it.render(g) }
        
        // Rendu des explosions
        explosions.forEach { it.render(g) }
        
        // Rendu des effets de combo
        comboEffects.forEach { it.render(g) }
    }
    
    fun reset() {
        enemies.clear()
        projectiles.clear()
        powerUps.clear()
        explosions.clear()
        comboEffects.clear()
        
        score = 0
        wave = 1
        difficulty = 1.0f
        waveTimer = 0
        enemySpawnCount = 0
        
        player.health = player.maxHealth
        player.ammo = player.maxAmmo
        player.shield = 0
        player.level = 1
        player.experience = 0
        
        currentCombo = 0
        comboTimer = 0
        
        weatherSystem.reset()
        mapSystem.reset()
        
        isPaused = false
        isGameOver = false
        
        GameLogger.info("🔄 Jeu réinitialisé")
    }
    
    fun pause() {
        isPaused = !isPaused
        if (isPaused) {
            GameLogger.info("⏸️ Jeu en pause")
        } else {
            GameLogger.info("▶️ Jeu repris")
        }
    }
}
