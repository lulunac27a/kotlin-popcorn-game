import java.util.Random
import java.util.Scanner
import kotlin.math.abs

fun main() {
    val scanner = Scanner(System.`in`)
    val random = Random()
    
    val width = 20
    val height = 12
    var bucketX = width / 2
    var score = 0
    var missed = 0
    val maxMissed = 3
    var lastEventMessage = "Game Started! Catch the popcorn!"

    // Active popcorns on the screen
    val popcorns = mutableListOf<Popcorn>()

    println("==================================================")
    println("        KOTLIN POPCORN GAME: SCORE EDITION        ")
    println("==================================================")
    println("Controls:")
    println("  'a' = Move Left | 'd' = Move Right | 'w' = Wait | 'q' = Quit")
    println("\nPositional Scoring:")
    println("  * Center Bucket Catch : +25 pts (Bullseye!)")
    println("  * Rim Bucket Catch    : +10 pts")
    println("  * Outer Edge Kernel @ : 2x Point Multiplier!")
    println("==================================================\n")
    print("Press Enter to Start...")
    scanner.nextLine()

    while (missed < maxMissed) {
        // Clear screen / Draw header
        print("\u001b[H\u001b[2J")
        System.out.flush()
        println("Score: $score | Missed: $missed/$maxMissed")
        println("Status: $lastEventMessage")

        // 35% chance to spawn a new piece of popcorn at the top
        if (random.nextFloat() < 0.35) {
            val spawnX = random.nextInt(width)
            // Kernels near the left or right walls are high-value edge kernels
            val isEdgeBonus = spawnX <= 1 || spawnX >= width - 2
            popcorns.add(Popcorn(x = spawnX, y = 0, isEdgeBonus = isEdgeBonus))
        }

        // Draw the game board
        for (y in 0 until height) {
            for (x in 0 until width) {
                val kernel = popcorns.find { it.x == x && it.y == y }
                when {
                    // Draw the bucket at the bottom row
                    y == height - 1 && x == bucketX -> print("[U]")
                    y == height - 1 && (x == bucketX - 1 || x == bucketX + 1) -> {
                        // Skip spaces occupied by the 3-character bucket [U]
                    }
                    // Draw active popcorns ('@' for 2x edge kernels, '*' for normal)
                    kernel != null -> print(if (kernel.isEdgeBonus) "@" else "*")
                    // Draw empty air
                    else -> print(" ")
                }
            }
            println()
        }
        
        // Draw the floor border with edge markers
        println("==" + "-".repeat(width - 2) + "==")

        // Get player input
        print("Move (a/d/w/q): ")
        val input = scanner.next().lowercase()

        if (input == "q") {
            println("Exiting game. Thanks for playing!")
            return
        }

        // Handle bucket movement mechanics
        when (input) {
            "a" -> if (bucketX > 1) bucketX--
            "d" -> if (bucketX < width - 2) bucketX++
        }

        // Advance popcorn positions downwards
        val iterator = popcorns.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.y++

            // Check if popcorn reaches bucket level
            if (p.y >= height - 1) {
                val distanceFromCenter = abs(p.x - bucketX)

                if (distanceFromCenter <= 1) {
                    // Base points depend on exact position inside the bucket
                    val basePoints = if (distanceFromCenter == 0) 25 else 10
                    val multiplier = if (p.isEdgeBonus) 2 else 1
                    val earned = basePoints * multiplier
                    
                    score += earned
                    
                    val catchType = if (distanceFromCenter == 0) "CENTER BULLSEYE" else "Rim Catch"
                    val bonusTag = if (p.isEdgeBonus) " (2x Edge Bonus!)" else ""
                    lastEventMessage = "+$earned pts! $catchType$bonusTag"
                } else {
                    missed++
                    lastEventMessage = "Missed a kernel at column ${p.x}!"
                }
                iterator.remove()
            }
        }
    }

    println("\n💥 GAME OVER! 💥")
    println("Final Popping Score: $score points")
}

data class Popcorn(
    var x: Int,
    var y: Int,
    val isEdgeBonus: Boolean = false
)
