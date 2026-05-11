package com.aircraftwar.ui

import javax.swing.JFrame

/**
 * Fenêtre principale du jeu
 */
class GameWindow : JFrame("✈️ Aircraft War Pro v2.0.0") {
    
    private val gamePanel: GamePanel
    
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        
        gamePanel = GamePanel()
        add(gamePanel)
        
        setSize(800, 600)
        setLocationRelativeTo(null)
        
        gamePanel.requestFocusInWindow()
    }
}
