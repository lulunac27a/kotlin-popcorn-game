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
    var comboStreak = 0
    var lastEventMessage = "Game Started! Catch the popcorn!"

    val popcorns = mutableListOf<Popcorn>()

    println("=======================================================")
    println("     KOTLIN POPCORN GAME: COMBO & GOLDEN EDITION       ")
    println("=======================================================")
    println("Controls:")
    println("  'a' = Move Left | 'd' = Move Right | 'w' = Wait | 'q' = Quit")
    println("\nKernel Types:")
    println("  * Standard Kernel : Falls 1 row  | Base 10-25 pts")
    println("  @ Edge Kernel     : Falls 1 row  | 2x Lane Bonus")
    println("  $ Golden Kernel   : Falls 2 rows | Base 50 pts!")
    println("\nCombo Rule:")
    println("  Consecutive Center Bullseyes multiply your score!")
    println("=======================================================\n")
    print("Press Enter to Start...")
    scanner.nextLine()

    while (missed < maxMissed) {
        // Clear screen / Draw header
        print("\u001b[H\u001b[2J")
        System.out.flush()
        println("Score: $score | Combo: ${comboStreak}x | Missed: $missed/$maxMissed")
        println("Status: $lastEventMessage")

        // 40% chance to spawn a new piece of popcorn at the top
        if (random.nextFloat() < 0.40) {
            val spawnX = random.nextInt(width)
            val isEdgeBonus = spawnX <= 1 || spawnX >= width - 2
            val isGolden = random.nextFloat() < 0.15 // 15% chance for a fast golden kernel

            popcorns.add(
                    Popcorn(
                            x = spawnX,
                            y = 0,
                            speed = if (isGolden) 2 else 1,
                            isEdgeBonus = isEdgeBonus,
                            isGolden = isGolden
                    )
            )
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
                    // Draw active popcorns
                    kernel != null -> {
                        val symbol =
                                when {
                                    kernel.isGolden -> "$"
                                    kernel.isEdgeBonus -> "@"
                                    else -> "*"
                                }
                        print(symbol)
                    }
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
            p.y += p.speed

            // Check if popcorn reaches or passes bucket level
            if (p.y >= height - 1) {
                val distanceFromCenter = abs(p.x - bucketX)

                if (distanceFromCenter <= 1) {
                    val isBullseye = distanceFromCenter == 0

                    // Update combo streak
                    if (isBullseye) {
                        comboStreak++
                    } else {
                        comboStreak = 0
                    }

                    // Calculate base points
                    val basePoints =
                            when {
                                p.isGolden -> 50
                                isBullseye -> 25
                                else -> 10
                            }

                    // Apply edge & combo multipliers
                    val edgeMultiplier = if (p.isEdgeBonus) 2 else 1
                    val comboMultiplier = if (comboStreak > 1) comboStreak else 1
                    val earned = basePoints * edgeMultiplier * comboMultiplier

                    score += earned

                    // Build feedback message
                    val typeLabel =
                            when {
                                p.isGolden -> "GOLDEN CATCH"
                                isBullseye -> "CENTER BULLSEYE"
                                else -> "Rim Catch"
                            }
                    val tags = mutableListOf<String>()
                    if (p.isEdgeBonus) tags.add("2x Edge")
                    if (comboStreak > 1) tags.add("${comboStreak}x Combo")
                    val tagText = if (tags.isNotEmpty()) " (${tags.joinToString(", ")})" else ""

                    lastEventMessage = "+$earned pts! $typeLabel$tagText"
                } else {
                    missed++
                    comboStreak = 0
                    val kernelName = if (p.isGolden) "Golden kernel" else "kernel"
                    lastEventMessage = "Missed a $kernelName at column ${p.x}! Combo reset."
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
        val speed: Int = 1,
        val isEdgeBonus: Boolean = false,
        val isGolden: Boolean = false
)
