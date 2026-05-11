package com.aircraftwar.utils

import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Système de logging centralisé pour le jeu
 */
object GameLogger {
    private val logger = LoggerFactory.getLogger("AircraftWar")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    fun info(message: String) {
        val timestamp = LocalDateTime.now().format(timeFormatter)
        println("[$timestamp] ℹ️  $message")
        logger.info(message)
    }
    
    fun debug(message: String) {
        val timestamp = LocalDateTime.now().format(timeFormatter)
        println("[$timestamp] 🔍 $message")
        logger.debug(message)
    }
    
    fun warn(message: String) {
        val timestamp = LocalDateTime.now().format(timeFormatter)
        println("[$timestamp] ⚠️  $message")
        logger.warn(message)
    }
    
    fun error(message: String, throwable: Throwable? = null) {
        val timestamp = LocalDateTime.now().format(timeFormatter)
        println("[$timestamp] ❌ $message")
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }
}
