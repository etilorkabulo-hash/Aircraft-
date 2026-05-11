package com.aircraftwar.ui

import com.aircraftwar.game.GameManager
import com.aircraftwar.utils.GameLogger
import com.aircraftwar.world.TimeOfDay
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import kotlin.system.exitProcess

/**
 * Panneau principal avec rendu et gestion des inputs
 */
class GamePanel : JPanel() {
    
    private val gameManager = GameManager(800, 600)
    
    private var lastFireTime = 0L
    private val fireDelay = 100L
    
    private var frameCount = 0
    private var lastFpsUpdate = 0L
    private var currentFps = 0
    
    private val keyPressed = mutableSetOf<Int>()
    
    init {
        background = Color(20, 20, 40)
        isFocusable = true
        
        setupKeyListener()
        startGameLoop()
    }
    
    private fun setupKeyListener() {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                keyPressed.add(e.keyCode)
                
                when (e.keyCode) {
                    KeyEvent.VK_SPACE -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastFireTime > fireDelay) {
                            gameManager.fireProjectile()
                            lastFireTime = currentTime
                        }
                    }
                    KeyEvent.VK_P -> gameManager.pause()
                    KeyEvent.VK_R -> {
                        if (gameManager.isGameOver) {
                            gameManager.reset()
                        }
                    }
                    KeyEvent.VK_ESCAPE -> {
                        GameLogger.info("👋 Fermeture du jeu")
                        exitProcess(0)
                    }
                }
            }
            
            override fun keyReleased(e: KeyEvent) {
                keyPressed.remove(e.keyCode)
            }
        })
    }
    
    private fun startGameLoop() {
        Thread {
            var lastTime = System.nanoTime()
            val targetFps = 60
            val ns = 1_000_000_000L / targetFps
            
            while (true) {
                val now = System.nanoTime()
                val elapsed = now - lastTime
                
                if (elapsed >= ns) {
                    updateInput()
                    gameManager.update()
                    gameManager.enemyShoot()
                    repaint()
                    
                    lastTime = now
                    
                    frameCount++
                    val currentTimeMs = System.currentTimeMillis()
                    if (currentTimeMs - lastFpsUpdate >= 1000) {
                        currentFps = frameCount
                        frameCount = 0
                        lastFpsUpdate = currentTimeMs
                    }
                }
                
                Thread.sleep(1)
            }
        }.apply {
            isDaemon = true
            start()
        }
    }
    
    private fun updateInput() {
        val moveUp = KeyEvent.VK_UP in keyPressed
        val moveDown = KeyEvent.VK_DOWN in keyPressed
        val moveLeft = KeyEvent.VK_LEFT in keyPressed
        val moveRight = KeyEvent.VK_RIGHT in keyPressed
        
        gameManager.player.setDirection(moveUp, moveDown, moveLeft, moveRight)
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        
        // Rendu du jeu
        gameManager.render(g2d)
        
        // Rendu de l'UI
        renderUI(g2d)
        
        // États du jeu
        if (gameManager.isPaused) {
            renderPauseMenu(g2d)
        }
        
        if (gameManager.isGameOver) {
            renderGameOver(g2d)
        }
    }
    
    private fun renderUI(g2d: Graphics2D) {
        g2d.font = Font("Arial", Font.BOLD, 14)
        
        // Ligne 1: Score et Wave
        g2d.color = Color(0, 255, 100)
        g2d.drawString("Score: ${gameManager.score}", 15, 25)
        g2d.drawString("Wave: ${gameManager.wave}", 15, 45)
        g2d.drawString("Diff: ${String.format("%.2f", gameManager.difficulty)}x", 15, 65)
        
        // Ligne 2: Niveau et EXP
        g2d.color = Color(200, 200, 100)
        g2d.drawString("Level: ${gameManager.player.level}", 15, 85)
        val expPercent = (gameManager.player.experience.toFloat() / (100 * gameManager.player.level) * 100).toInt()
        g2d.drawString("EXP: $expPercent%", 15, 105)
        
        // Droite: Santé
        g2d.color = Color(255, 100, 100)
        g2d.drawString("Health: ${gameManager.player.health}/${gameManager.player.maxHealth}", 600, 25)
        
        // Droite: Munitions
        g2d.color = Color(255, 200, 50)
        g2d.drawString("Ammo: ${gameManager.player.ammo}/${gameManager.player.maxAmmo}", 600, 45)
        
        // Droite: Bouclier
        if (gameManager.player.shield > 0) {
            g2d.color = Color(100, 200, 255)
            g2d.drawString("Shield: ${gameManager.player.shield}", 600, 65)
        }
        
        // Combo
        if (gameManager.currentCombo > 1) {
            g2d.color = Color(255, 150, 0)
            g2d.font = Font("Arial", Font.BOLD, 18)
            g2d.drawString("Combo x${gameManager.currentCombo}!", 320, 40)
        }
        
        // Méteo et Terrains
        g2d.font = Font("Arial", Font.PLAIN, 12)
        g2d.color = Color(150, 200, 255)
        g2d.drawString(gameManager.weatherSystem.getWeatherInfo(), 15, 560)
        g2d.drawString("${gameManager.mapSystem.currentTerrain.symbol} - ${gameManager.mapSystem.timeOfDay.symbol}", 
                       15, 575)
        
        // Stats ennemis
        g2d.color = Color(200, 100, 100)
        g2d.drawString("Ennemis: ${gameManager.enemies.size}", 600, 85)
        
        // FPS
        g2d.color = Color(100, 200, 100)
        g2d.drawString("FPS: $currentFps", 720, 590)
        
        // Contrôles
        g2d.color = Color(100, 150, 150)
        g2d.font = Font("Arial", Font.PLAIN, 10)
        g2d.drawString("↑↓←→ MOVE | SPACE SHOOT | P PAUSE | R RESTART | ESC EXIT", 20, 600)
    }
    
    private fun renderPauseMenu(g2d: Graphics2D) {
        // Fond semi-transparent
        g2d.color = Color(0, 0, 0, 200)
        g2d.fillRect(0, 0, width, height)
        
        // Titre
        g2d.color = Color(255, 255, 100)
        g2d.font = Font("Arial", Font.BOLD, 60)
        var text = "⏸ PAUSE"
        var metrics = g2d.fontMetrics
        var x = (width - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, height / 2 - 40)
        
        // Infos
        g2d.color = Color(200, 200, 200)
        g2d.font = Font("Arial", Font.PLAIN, 18)
        
        text = "Score: ${gameManager.score} | Wave: ${gameManager.wave}"
        metrics = g2d.fontMetrics
        x = (width - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, height / 2 + 30)
        
        // Instructions
        g2d.color = Color(100, 255, 100)
        g2d.font = Font("Arial", Font.PLAIN, 16)
        g2d.drawString("Appuyez sur P pour continuer", (width - 200) / 2, height / 2 + 80)
    }
    
    private fun renderGameOver(g2d: Graphics2D) {
        // Fond semi-transparent
        g2d.color = Color(0, 0, 0, 230)
        g2d.fillRect(0, 0, width, height)
        
        // Titre
        g2d.color = Color(255, 50, 50)
        g2d.font = Font("Arial", Font.BOLD, 56)
        var text = "☠ GAME OVER ☠"
        var metrics = g2d.fontMetrics
        var x = (width - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, height / 2 - 80)
        
        // Stats
        g2d.color = Color(255, 255, 100)
        g2d.font = Font("Arial", Font.BOLD, 20)
        
        text = "Score Final: ${gameManager.score}"
        metrics = g2d.fontMetrics
        x = (width - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, height / 2 - 20)
        
        g2d.color = Color(200, 200, 200)
        g2d.font = Font("Arial", Font.PLAIN, 14)
        
        text = "Vagues: ${gameManager.wave} | Ennemis: ${gameManager.totalEnemiesDefeated} | Dégâts: ${gameManager.totalDamageDealt}"
        metrics = g2d.fontMetrics
        x = (width - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, height / 2 + 20)
        
        text = "Combo Max: ${gameManager.longestCombo} | Niveau: ${gameManager.player.level}"
        metrics = g2d.fontMetrics
        x = (width - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, height / 2 + 45)
        
        // Redémarrage
        g2d.color = Color(100, 255, 100)
        g2d.font = Font("Arial", Font.BOLD, 18)
        g2d.drawString("Appuyez sur R pour recommencer", (width - 250) / 2, height / 2 + 120)
    }
}
