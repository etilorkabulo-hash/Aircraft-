package com.aircraftwar.game

import com.aircraftwar.world.WeatherSystem
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Classe de base pour toutes les entités du jeu
 */
abstract class GameEntity(
    open var x: Double,
    open var y: Double,
    open val width: Double,
    open val height: Double
) {
    abstract fun update(weatherSystem: WeatherSystem? = null)
    abstract fun render(g: Graphics2D)
    
    open fun getBounds(): Rectangle2D.Double = Rectangle2D.Double(x, y, width, height)
    
    fun collidesWith(other: GameEntity): Boolean {
        return getBounds().intersects(other.getBounds())
    }
    
    fun distanceTo(other: GameEntity): Double {
        val dx = (x + width / 2) - (other.x + other.width / 2)
        val dy = (y + height / 2) - (other.y + other.height / 2)
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Joueur - Vaisseau principal
 */
class Player(
    x: Double = 375.0,
    y: Double = 500.0,
    private val screenWidth: Int = 800,
    private val screenHeight: Int = 600
) : GameEntity(x, y, 50.0, 50.0) {
    
    override var x = x
    override var y = y
    
    var health = 150
    var maxHealth = 150
    var ammo = 200
    var maxAmmo = 200
    var shield = 0
    var maxShield = 100
    
    // Stats avancées
    var combo = 0
    var experience = 0
    var level = 1
    
    private var velocityX = 0.0
    private var velocityY = 0.0
    private val baseSpeed = 6.0
    var currentSpeed = baseSpeed
    
    var moveUp = false
    var moveDown = false
    var moveLeft = false
    var moveRight = false
    
    // Dégâts bonus par type de météo
    var damageMultiplier = 1.0f
    
    fun setDirection(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        moveUp = up
        moveDown = down
        moveLeft = left
        moveRight = right
    }
    
    override fun update(weatherSystem: WeatherSystem?) {
        velocityX = 0.0
        velocityY = 0.0
        
        // Vitesse réduite par mauvais temps
        currentSpeed = baseSpeed * (weatherSystem?.visibility ?: 1.0f)
        
        if (moveLeft && x > 0) velocityX = -currentSpeed
        if (moveRight && x < screenWidth - width) velocityX = currentSpeed
        if (moveUp && y > screenHeight / 2) velocityY = -currentSpeed
        if (moveDown && y < screenHeight - height) velocityY = currentSpeed
        
        // Effet du vent
        weatherSystem?.let { weather ->
            x += weather.windForce * 0.5
        }
        
        x += velocityX
        y += velocityY
        
        // Limites d'écran
        x = x.coerceIn(0.0, (screenWidth - width).toDouble())
        y = y.coerceIn((screenHeight / 2).toDouble(), (screenHeight - height).toDouble())
        
        // Régénération lente du bouclier
        if (shield < maxShield && shield > 0) {
            shield = (shield + 0.5).toInt().coerceAtMost(maxShield)
        }
    }
    
    override fun render(g: Graphics2D) {
        // Corps du vaisseau
        val xPoints = intArrayOf(
            (x + width / 2).toInt(),
            (x).toInt(),
            (x + width).toInt()
        )
        val yPoints = intArrayOf(
            y.toInt(),
            (y + height).toInt(),
            (y + height).toInt()
        )
        
        g.color = Color(0, 200, 100)
        g.fillPolygon(xPoints, yPoints, 3)
        
        g.color = Color(0, 255, 150)
        g.strokePolygon(xPoints, yPoints, 3)
        
        // Effet de thrust
        g.color = Color(255, 100, 0, 150)
        g.fillRect((x + 15).toInt(), (y + height + 5).toInt(), 20, 10)
        
        // Bouclier si actif
        if (shield > 0) {
            val shieldAlpha = (shield.toFloat() / maxShield * 200).toInt()
            g.color = Color(100, 150, 255, shieldAlpha)
            g.drawOval((x - 8).toInt(), (y - 8).toInt(), (width + 16).toInt(), (height + 16).toInt())
            
            // Barre de bouclier
            g.color = Color(100, 200, 255)
            g.fillRect((x - 5).toInt(), (y - 20).toInt(), (shield / maxShield * 50).toInt(), 5)
            g.color = Color.WHITE
            g.drawRect((x - 5).toInt(), (y - 20).toInt(), 50, 5)
        }
    }
    
    fun takeDamage(amount: Int) {
        if (shield > 0) {
            val shieldDamage = (amount * 0.7).toInt()
            shield = (shield - shieldDamage).coerceAtLeast(0)
            health -= (amount - shieldDamage)
        } else {
            health -= amount
        }
    }
    
    fun heal(amount: Int) {
        health = (health + amount).coerceAtMost(maxHealth)
    }
    
    fun gainExperience(amount: Int) {
        experience += amount
        val expPerLevel = 100 * level
        if (experience >= expPerLevel) {
            levelUp()
        }
    }
    
    private fun levelUp() {
        level++
        experience = 0
        maxHealth += 20
        maxAmmo += 50
        maxShield += 20
        health = maxHealth
        ammo = maxAmmo
    }
    
    fun isAlive(): Boolean = health > 0
}

/**
 * Ennemi avec IA avancée
 */
class Enemy(
    x: Double,
    y: Double,
    val type: EnemyType = EnemyType.BASIC,
    private val difficulty: Float = 1.0f
) : GameEntity(x, y, 35.0, 35.0) {
    
    override var x = x
    override var y = y
    
    var health = (type.health * difficulty).toInt()
    private var velocityX = (Math.random() - 0.5) * 3
    private val baseVelocityY = 2.0 * difficulty
    private var velocityY = baseVelocityY
    
    private var shootTimer = 0
    private val shootInterval = (type.shootInterval / difficulty).toInt()
    
    // IA
    private var aiTimer = 0
    private var targetX = x
    
    override fun update(weatherSystem: WeatherSystem?) {
        // Effet du vent
        weatherSystem?.let { weather ->
            velocityX += weather.windForce * 0.3f
        }
        
        x += velocityX
        y += velocityY
        
        // Logique d'IA simple
        aiTimer++
        if (aiTimer > 60) {
            targetX = Math.random() * 800
            aiTimer = 0
        }
        
        // Mouvements de base
        if (x < targetX && velocityX > -2) velocityX += 0.1
        if (x > targetX && velocityX < 2) velocityX -= 0.1
        
        shootTimer++
    }
    
    override fun render(g: Graphics2D) {
        // Corps ennemi
        g.color = type.color
        g.fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        
        g.color = Color(255, 100, 0)
        g.drawRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        
        // Barre de santé
        val healthPercent = health.toFloat() / (type.health * (1 + difficulty))
        g.color = Color.RED
        g.fillRect(x.toInt(), (y - 10).toInt(), width.toInt(), 4)
        
        g.color = Color.GREEN
        g.fillRect(x.toInt(), (y - 10).toInt(), (width * healthPercent).toInt(), 4)
    }
    
    fun shouldShoot(): Boolean {
        if (shootTimer >= shootInterval) {
            shootTimer = 0
            return true
        }
        return false
    }
    
    fun takeDamage(amount: Int) {
        health -= amount
    }
    
    fun isAlive(): Boolean = health > 0
    fun isOffScreen(): Boolean = y > 650
    
    fun getScore(): Int = (type.score * difficulty).toInt()
}

/**
 * Type d'ennemi avec statistiques
 */
enum class EnemyType(
    val health: Int,
    val shootInterval: Int,
    val score: Int,
    val color: Color,
    val symbol: String
) {
    BASIC(25, 120, 10, Color(255, 100, 100), "🔴"),
    STRONG(60, 90, 30, Color(255, 50, 50), "🔶"),
    FAST(20, 150, 15, Color(255, 150, 50), "🟠"),
    BOSS(200, 60, 150, Color(255, 0, 0), "👹")
}

/**
 * Projectile avec gravité et vent
 */
class Projectile(
    x: Double,
    y: Double,
    val isPlayerBullet: Boolean = true,
    var damageMultiplier: Float = 1.0f
) : GameEntity(x, y, 6.0, 15.0) {
    
    override var x = x
    override var y = y
    
    private val baseVelocityY = if (isPlayerBullet) -12.0 else 8.0
    private var velocityY = baseVelocityY
    private var velocityX = 0.0
    
    val baseDamage = if (isPlayerBullet) 20 else 12
    val damage: Int
        get() = (baseDamage * damageMultiplier).toInt()
    
    private var lifeTime = 0
    private val maxLifeTime = 300
    
    override fun update(weatherSystem: WeatherSystem?) {
        // Effet du vent
        weatherSystem?.let { weather ->
            velocityX += weather.windForce * 0.2
        }
        
        // Gravité légère
        velocityY += 0.2
        
        x += velocityX
        y += velocityY
        
        lifeTime++
    }
    
    override fun render(g: Graphics2D) {
        g.color = if (isPlayerBullet) Color(0, 255, 100) else Color(255, 150, 50)
        g.fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        
        // Traînée
        g.color = if (isPlayerBullet) Color(0, 150, 50, 100) else Color(200, 100, 50, 100)
        g.fillRect(x.toInt(), (y + height).toInt(), width.toInt(), 3)
    }
    
    fun isOffScreen(): Boolean = y < -20 || y > 650 || lifeTime > maxLifeTime
}

/**
 * Power-up avec animations
 */
class PowerUp(
    x: Double,
    y: Double,
    val type: PowerUpType
) : GameEntity(x, y, 30.0, 30.0) {
    
    override var x = x
    override var y = y
    
    private val velocityY = 1.5
    private var floatOffset = 0f
    private var rotation = 0f
    
    override fun update(weatherSystem: WeatherSystem?) {
        y += velocityY
        floatOffset += 0.1f
        rotation += 5f
    }
    
    override fun render(g: Graphics2D) {
        g.color = type.color
        
        // Effet de flottement
        val offsetY = (y + Math.sin(floatOffset.toDouble()) * 5).toInt()
        
        g.fillRect(x.toInt(), offsetY, width.toInt(), height.toInt())
        
        g.color = Color.WHITE
        g.drawRect(x.toInt(), offsetY, width.toInt(), height.toInt())
        
        // Symbole
        g.color = Color.WHITE
        g.font = g.font.deriveFont(16f)
        g.drawString(type.symbol, (x + 8).toInt(), (offsetY + 20).toInt())
    }
    
    fun isOffScreen(): Boolean = y > 650
}

/**
 * Type de power-up
 */
enum class PowerUpType(
    val color: Color,
    val symbol: String,
    val rarityScore: Int,
    val effect: (Player) -> Unit
) {
    HEALTH(Color.GREEN, "❤", 10, { it.heal(40) }),
    AMMO(Color.YELLOW, "🔫", 15, { it.ammo = (it.ammo + 60).coerceAtMost(it.maxAmmo) }),
    SHIELD(Color.CYAN, "🛡", 20, { it.shield = it.maxShield }),
    DAMAGE_BOOST(Color(255, 150, 0), "⚡", 25, { it.damageMultiplier = 1.5f }),
    SPEED_BOOST(Color.MAGENTA, "💨", 22, { it.currentSpeed = it.baseSpeed * 1.3f })
}

/**
 * Explosion avec particules
 */
class Explosion(
    x: Double,
    y: Double,
    private val intensity: Int = 20
) : GameEntity(x, y, 60.0, 60.0) {
    
    override var x = x
    override var y = y
    
    private var life = intensity
    
    override fun update(weatherSystem: WeatherSystem?) {
        life--
    }
    
    override fun render(g: Graphics2D) {
        val alpha = (255 * life / intensity).coerceIn(0, 255)
        
        for (i in 0 until life step 2) {
            g.color = Color(255, 200 - i * 5, 50, alpha)
            g.fillOval((x - i).toInt(), (y - i).toInt(), (width + i * 2).toInt(), (height + i * 2).toInt())
        }
    }
    
    fun isAlive(): Boolean = life > 0
}

/**
 * Bonus de combo visual
 */
class ComboEffect(
    val x: Double,
    val y: Double,
    val combo: Int
) : GameEntity(x, y, 50.0, 30.0) {
    
    override var x = x
    override var y = y
    
    private var life = 60
    
    override fun update(weatherSystem: WeatherSystem?) {
        y -= 1.5
        life--
    }
    
    override fun render(g: Graphics2D) {
        val alpha = (255 * life / 60).toInt()
        g.color = Color(255, 200, 0, alpha)
        g.font = g.font.deriveFont(20f)
        g.drawString("Combo x$combo!", x.toInt(), y.toInt())
    }
    
    fun isAlive(): Boolean = life > 0
}
