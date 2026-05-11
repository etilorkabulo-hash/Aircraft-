package com.aircraftwar.game

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe

class GameManagerTest : StringSpec({
    
    "GameManager should initialize correctly" {
        val gameManager = GameManager()
        gameManager.score shouldBe 0
        gameManager.wave shouldBe 1
        gameManager.difficulty shouldBe 1.0f
        gameManager.isPaused shouldBe false
        gameManager.isGameOver shouldBe false
    }
    
    "Player should take damage and lose health" {
        val gameManager = GameManager()
        val initialHealth = gameManager.player.health
        gameManager.player.takeDamage(25)
        gameManager.player.health shouldBe initialHealth - 25
    }
    
    "Player should be able to fire projectiles" {
        val gameManager = GameManager()
        val initialAmmo = gameManager.player.ammo
        gameManager.fireProjectile()
        gameManager.player.ammo shouldBe initialAmmo - 1
    }
    
    "Game should handle pause state" {
        val gameManager = GameManager()
        gameManager.isPaused shouldBe false
        gameManager.pause()
        gameManager.isPaused shouldBe true
        gameManager.pause()
        gameManager.isPaused shouldBe false
    }
    
    "Player should gain experience and level up" {
        val gameManager = GameManager()
        val initialLevel = gameManager.player.level
        gameManager.player.gainExperience(150)
        gameManager.player.level shouldBeGreaterThan initialLevel
    }
    
    "Reset should restore game to initial state" {
        val gameManager = GameManager()
        gameManager.score = 5000
        gameManager.wave = 10
        gameManager.player.takeDamage(50)
        
        gameManager.reset()
        
        gameManager.score shouldBe 0
        gameManager.wave shouldBe 1
        gameManager.difficulty shouldBe 1.0f
        gameManager.player.health shouldBe gameManager.player.maxHealth
        gameManager.isPaused shouldBe false
        gameManager.isGameOver shouldBe false
    }
})
