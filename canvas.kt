import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.*
import kotlin.math.abs
import kotlin.random.Random

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Kotlin Popcorn Game - Mouse & Keyboard Edition")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isResizable = false
        frame.add(PopcornGamePanel())
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}

class PopcornGamePanel : JPanel() {
    private val boardWidth = 600
    private val boardHeight = 700
    private val bucketWidth = 90
    private val bucketHeight = 50
    private val edgeZoneWidth = 80

    private var bucketX = (boardWidth - bucketWidth) / 2
    private val bucketY = boardHeight - bucketHeight - 25
    private val bucketSpeed = 9

    private var leftPressed = false
    private var rightPressed = false

    private var score = 0
    private var missed = 0
    private val maxMissed = 3
    private var comboStreak = 0
    private var isGameOver = false

    private val popcorns = mutableListOf<Kernel>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private var spawnCounter = 0

    // 60 FPS Game Loop (~16ms per tick)
    private val gameTimer = Timer(16) {
        updateGame()
        repaint()
    }

    init {
        preferredSize = Dimension(boardWidth, boardHeight)
        background = Color(25, 25, 35)
        isFocusable = true

        // Mouse Motion Listener: Center bucket horizontally on mouse cursor
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                moveBucketToMouse(e.x)
            }

            override fun mouseDragged(e: MouseEvent) {
                moveBucketToMouse(e.x)
            }
        })

        // Mouse Click Listener: Click anywhere to restart when Game Over
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (isGameOver) {
                    resetGame()
                }
            }
        })

        // Keyboard Listener: Optional fallback controls & 'R' restart
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true
                    KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true
                    KeyEvent.VK_R -> if (isGameOver) resetGame()
                }
            }

            override fun keyReleased(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = false
                    KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = false
                }
            }
        })

        gameTimer.start()
    }

    private fun moveBucketToMouse(mouseX: Int) {
        if (isGameOver) return
        val centeredX = mouseX - (bucketWidth / 2)
        bucketX = centeredX.coerceIn(0, boardWidth - bucketWidth)
    }

    private fun resetGame() {
        score = 0
        missed = 0
        comboStreak = 0
        isGameOver = false
        popcorns.clear()
        floatingTexts.clear()
        bucketX = (boardWidth - bucketWidth) / 2
    }

    private fun updateGame() {
        if (isGameOver) return

        // 1. Keyboard horizontal bucket movement (works alongside mouse)
        if (leftPressed && bucketX > 0) {
            bucketX = (bucketX - bucketSpeed).coerceAtLeast(0)
        }
        if (rightPressed && bucketX < boardWidth - bucketWidth) {
            bucketX = (bucketX + bucketSpeed).coerceAtMost(boardWidth - bucketWidth)
        }

        // 2. Spawn new kernels periodically
        spawnCounter++
        if (spawnCounter >= 42) {
            spawnCounter = 0
            val radius = 12
            val spawnX = Random.nextInt(radius + 10, boardWidth - radius - 10)
            val isEdgeBonus = spawnX <= edgeZoneWidth || spawnX >= boardWidth - edgeZoneWidth
            val isGolden = Random.nextFloat() < 0.18f

            popcorns.add(
                Kernel(
                    x = spawnX.toDouble(),
                    y = -20.0,
                    radius = radius,
                    speedY = if (isGolden) 6.5 else 3.8,
                    isEdgeBonus = isEdgeBonus,
                    isGolden = isGolden
                )
            )
        }

        // 3. Update kernels and check collisions
        val bucketCenterX = bucketX + bucketWidth / 2.0
        val iterator = popcorns.iterator()

        while (iterator.hasNext()) {
            val k = iterator.next()
            k.y += k.speedY

            // Check catch at bucket rim height
            if (k.y + k.radius >= bucketY && k.y - k.radius <= bucketY + 20) {
                val distFromCenter = abs(k.x - bucketCenterX)

                if (distFromCenter <= bucketWidth / 2.0) {
                    // Bullseye if within inner 24 pixels of bucket center
                    val isBullseye = distFromCenter <= 12.0

                    if (isBullseye) {
                        comboStreak++
                    } else {
                        comboStreak = 0
                    }

                    val basePoints = when {
                        k.isGolden -> 50
                        isBullseye -> 25
                        else -> 10
                    }

                    val edgeMult = if (k.isEdgeBonus) 2 else 1
                    val comboMult = if (comboStreak > 1) comboStreak else 1
                    val earned = basePoints * edgeMult * comboMult
                    score += earned

                    val label = buildString {
                        append("+$earned")
                        if (isBullseye) append(" BULLSEYE!")
                        if (k.isEdgeBonus) append(" (2x Edge)")
                        if (comboStreak > 1) append(" [${comboStreak}x]")
                    }

                    val popupColor = when {
                        k.isGolden -> Color(255, 215, 0)
                        isBullseye -> Color(100, 255, 140)
                        else -> Color.WHITE
                    }

                    floatingTexts.add(FloatingText(label, k.x.toInt(), bucketY - 10, popupColor))
                    iterator.remove()
                    continue
                }
            }

            // Check if kernel fell past bottom of screen
            if (k.y - k.radius > boardHeight) {
                missed++
                comboStreak = 0
                floatingTexts.add(FloatingText("MISS!", k.x.toInt(), boardHeight - 40, Color(255, 90, 90)))
                iterator.remove()

                if (missed >= maxMissed) {
                    isGameOver = true
                }
            }
        }

        // 4. Update floating score popups
        val textIter = floatingTexts.iterator()
        while (textIter.hasNext()) {
            val ft = textIter.next()
            ft.y -= 1
            ft.life--
            if (ft.life <= 0) textIter.remove()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draw 2x Edge Bonus Zones on left and right sides
        g2.color = Color(255, 215, 0, 18)
        g2.fillRect(0, 0, edgeZoneWidth, boardHeight)
        g2.fillRect(boardWidth - edgeZoneWidth, 0, edgeZoneWidth, boardHeight)

        g2.color = Color(255, 215, 0, 60)
        g2.font = Font("SansSerif", Font.BOLD, 12)
        g2.drawString("2x ZONE", 14, boardHeight - 10)
        g2.drawString("2x ZONE", boardWidth - edgeZoneWidth + 14, boardHeight - 10)

        // Draw Falling Popcorn Kernels
        for (k in popcorns) {
            val drawX = (k.x - k.radius).toInt()
            val drawY = (k.y - k.radius).toInt()
            val diameter = k.radius * 2

            g2.color = when {
                k.isGolden -> Color(255, 200, 0)       // Gold
                k.isEdgeBonus -> Color(255, 165, 90)   // Warm Orange-Cream
                else -> Color(255, 248, 220)           // Classic Popcorn Cream
            }
            g2.fillOval(drawX, drawY, diameter, diameter)

            // Fluffy popcorn highlight bump
            g2.color = Color(255, 255, 255, 180)
            g2.fillOval(drawX + 4, drawY + 3, k.radius / 2, k.radius / 2)
        }

        // Draw Striped Popcorn Bucket
        g2.color = Color(220, 45, 45)
        g2.fillRoundRect(bucketX, bucketY, bucketWidth, bucketHeight, 10, 10)

        // White vertical bucket stripes
        g2.color = Color.WHITE
        g2.fillRect(bucketX + 18, bucketY + 4, 12, bucketHeight - 6)
        g2.fillRect(bucketX + 40, bucketY + 4, 12, bucketHeight - 6)
        g2.fillRect(bucketX + 62, bucketY + 4, 12, bucketHeight - 6)

        // Center Bullseye Target Indicator on Bucket Rim
        g2.color = Color(80, 255, 120)
        g2.fillRect(bucketX + (bucketWidth / 2) - 12, bucketY - 3, 24, 5)

        // Draw Floating Score Popups
        g2.font = Font("SansSerif", Font.BOLD, 14)
        for (ft in floatingTexts) {
            g2.color = ft.color
            g2.drawString(ft.text, ft.x - 30, ft.y)
        }

        // Draw HUD (Score, Combo, Lives)
        g2.color = Color.WHITE
        g2.font = Font("SansSerif", Font.BOLD, 20)
        g2.drawString("Score: $score", 20, 32)

        g2.color = if (comboStreak > 1) Color(100, 255, 140) else Color.LIGHT_GRAY
        g2.drawString("Combo: ${comboStreak}x", 240, 32)

        g2.color = Color(255, 100, 100)
        g2.drawString("Lives: ${maxMissed - missed}", boardWidth - 115, 32)

        // Draw Game Over Overlay
        if (isGameOver) {
            g2.color = Color(0, 0, 0, 190)
            g2.fillRect(0, 0, boardWidth, boardHeight)

            g2.color = Color(255, 85, 85)
            g2.font = Font("SansSerif", Font.BOLD, 44)
            g2.drawString("GAME OVER", boardWidth / 2 - 135, boardHeight / 2 - 30)

            g2.color = Color.WHITE
            g2.font = Font("SansSerif", Font.BOLD, 24)
            g2.drawString("Final Score: $score", boardWidth / 2 - 90, boardHeight / 2 + 15)

            g2.color = Color.LIGHT_GRAY
            g2.font = Font("SansSerif", Font.PLAIN, 18)
            g2.drawString("Click or Press 'R' to Play Again", boardWidth / 2 - 120, boardHeight / 2 + 55)
        }
    }
}

data class Kernel(
    var x: Double,
    var y: Double,
    val radius: Int,
    val speedY: Double,
    val isEdgeBonus: Boolean,
    val isGolden: Boolean
)

data class FloatingText(
    val text: String,
    var x: Int,
    var y: Int,
    val color: Color,
    var life: Int = 45
)
