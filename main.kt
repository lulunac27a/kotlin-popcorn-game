import java.util.Random
import java.util.Scanner

fun main() {
    val scanner = Scanner(System.`in`)
    val random = Random()
    
    val width = 20
    val height = 12
    var bucketX = width / 2
    var score = 0
    var missed = 0
    val maxMissed = 3

    // Active popcorns on the screen
    val popcorns = mutableListOf<Popcorn>()

    println("=========================================")
    println("      WELCOME TO KOTLIN POPCORN GAME!     ")
    println("=========================================")
    println("Instructions:")
    println("  Press 'a' to move Bucket [U] Left")
    println("  Press 'd' to move Bucket [U] Right")
    println("  Press 'w' to stay still / wait")
    println("  Press 'q' to Quit")
    println("Catch '*' (Popcorn) & Avoid missing them!")
    println("=========================================\n")
    print("Press Enter to Start...")
    scanner.nextLine()

    while (missed < maxMissed) {
        // Clear screen / Draw header
        print("\u001b[H\u001b[2J") // ANSI Escape sequence to clear console
        System.out.flush()
        println("Score: $score | Missed: $missed/$maxMissed")

        // 30% chance to spawn a new piece of popcorn at the top
        if (random.nextFloat() < 0.3) {
            popcorns.add(Popcorn(x = random.nextInt(width), y = 0))
        }

        // Draw the game board
        for (y in 0 until height) {
            for (x in 0 until width) {
                when {
                    // Draw the bucket at the bottom row
                    y == height - 1 && x == bucketX -> print("[U]")
                    y == height - 1 && (x == bucketX - 1 || x == bucketX + 1) -> {
                        // Skip printing empty spaces occupied by the width of bucket [U]
                    }
                    // Draw active popcorns
                    popcorns.any { it.x == x && it.y == y } -> print("*")
                    // Draw empty air
                    else -> print(" ")
                }
            }
            println()
        }
        
        // Draw the floor border
        println("=".repeat(width + 2))

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
                // If it hits within the bucket range
                if (p.x >= bucketX - 1 && p.x <= bucketX + 1) {
                    score += 10
                } else {
                    missed++
                }
                iterator.remove()
            }
        }
    }

    println("\n💥 GAME OVER! 💥")
    println("Final Popping Score: $score points")
}

data class Popcorn(var x: Int, var y: Int)
