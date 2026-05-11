package com.aircraftwar

import com.aircraftwar.ui.GameWindow
import com.aircraftwar.utils.GameLogger
import javax.swing.SwingUtilities

/**
 * Point d'entrée principal de Aircraft War Pro
 */
fun main() {
    GameLogger.info("🚀 Démarrage d'Aircraft War Pro v2.0.0")
    
    SwingUtilities.invokeLater {
        try {
            val gameWindow = GameWindow()
            gameWindow.isVisible = true
            GameLogger.info("✅ Fenêtre de jeu initialisée")
        } catch (e: Exception) {
            GameLogger.error("❌ Erreur lors du démarrage du jeu", e)
            System.exit(1)
        }
    }
}
