import java.util.Random
import java.util.Scanner
import kotlin.math.abs

fun main() = PopcornGame().run()

private class PopcornGame {
    private val scanner = Scanner(System.`in`)
    private val random = Random()
    private val width = 20
    private val height = 12
    private val maxMissed = 3
    private val popcorns = mutableListOf<Popcorn>()

    private var bucketX = width / 2
    private var score = 0
    private var missed = 0
    private var comboStreak = 0
    private var lastEventMessage = "Game Started! Catch the popcorn!"

    fun run() {
        showInstructions()
        var quit = false
        while (missed < maxMissed && !quit) {
            quit = playTurn()
        }
        if (!quit) showGameOver()
    }

    private fun showInstructions() {
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
    }

    private fun playTurn(): Boolean {
        clearScreen()
        drawStatus()
        spawnPopcorn()
        drawBoard()

        val input = readInput()
        if (input == "q") {
            println("Exiting game. Thanks for playing!")
            return true
        }

        moveBucket(input)
        advancePopcorn()
        return false
    }

    private fun clearScreen() {
        print("\u001b[H\u001b[2J")
        System.out.flush()
    }

    private fun drawStatus() {
        println("Score: $score | Combo: ${comboStreak}x | Missed: $missed/$maxMissed")
        println("Status: $lastEventMessage")
    }

    private fun spawnPopcorn() {
        if (random.nextFloat() >= 0.40) return

        val spawnX = random.nextInt(width)
        val isEdgeBonus = spawnX <= 1 || spawnX >= width - 2
        val isGolden = random.nextFloat() < 0.15
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

    private fun drawBoard() {
        for (y in 0 until height) {
            for (x in 0 until width) drawCell(x, y)
            println()
        }
        println("==" + "-".repeat(width - 2) + "==")
    }

    private fun drawCell(x: Int, y: Int) {
        if (isBucketCenter(x, y)) {
            print("[U]")
            return
        }
        if (isBucketSide(x, y)) return

        val kernel = popcorns.find { it.x == x && it.y == y }
        print(kernel?.let(::kernelSymbol) ?: " ")
    }

    private fun isBucketCenter(x: Int, y: Int) = y == height - 1 && x == bucketX

    private fun isBucketSide(x: Int, y: Int) =
        y == height - 1 && (x == bucketX - 1 || x == bucketX + 1)

    private fun kernelSymbol(kernel: Popcorn): String = when {
        kernel.isGolden -> "$"
        kernel.isEdgeBonus -> "@"
        else -> "*"
    }

    private fun readInput(): String {
        print("Move (a/d/w/q): ")
        return scanner.next().lowercase()
    }

    private fun moveBucket(input: String) {
        when (input) {
            "a" -> if (bucketX > 1) bucketX--
            "d" -> if (bucketX < width - 2) bucketX++
        }
    }

    private fun advancePopcorn() {
        val iterator = popcorns.iterator()
        while (iterator.hasNext()) {
            val kernel = iterator.next()
            kernel.y += kernel.speed
            if (kernel.y >= height - 1) {
                handleLanding(kernel)
                iterator.remove()
            }
        }
    }

    private fun handleLanding(kernel: Popcorn) {
        if (abs(kernel.x - bucketX) <= 1) {
            recordCatch(kernel)
        } else {
            recordMiss(kernel)
        }
    }

    private fun recordCatch(kernel: Popcorn) {
        val isBullseye = kernel.x == bucketX
        updateCombo(isBullseye)

        val earned = calculatePoints(kernel, isBullseye)
        score += earned
        lastEventMessage = buildCatchMessage(kernel, isBullseye, earned)
    }

    private fun updateCombo(isBullseye: Boolean) {
        comboStreak = if (isBullseye) comboStreak + 1 else 0
    }

    private fun calculatePoints(kernel: Popcorn, isBullseye: Boolean): Int {
        val basePoints = when {
            kernel.isGolden -> 50
            isBullseye -> 25
            else -> 10
        }
        val edgeMultiplier = if (kernel.isEdgeBonus) 2 else 1
        val comboMultiplier = if (comboStreak > 1) comboStreak else 1
        return basePoints * edgeMultiplier * comboMultiplier
    }

    private fun buildCatchMessage(kernel: Popcorn, isBullseye: Boolean, earned: Int): String {
        val typeLabel = when {
            kernel.isGolden -> "GOLDEN CATCH"
            isBullseye -> "CENTER BULLSEYE"
            else -> "Rim Catch"
        }
        val tags = catchTags(kernel)
        val tagText = if (tags.isEmpty()) "" else " (${tags.joinToString(", ")})"
        return "+$earned pts! $typeLabel$tagText"
    }

    private fun catchTags(kernel: Popcorn): List<String> = buildList {
        if (kernel.isEdgeBonus) add("2x Edge")
        if (comboStreak > 1) add("${comboStreak}x Combo")
    }

    private fun recordMiss(kernel: Popcorn) {
        missed++
        comboStreak = 0
        val kernelName = if (kernel.isGolden) "Golden kernel" else "kernel"
        lastEventMessage = "Missed a $kernelName at column ${kernel.x}! Combo reset."
    }

    private fun showGameOver() {
        println("\n💥 GAME OVER! 💥")
        println("Final Popping Score: $score points")
    }
}

data class Popcorn(
    var x: Int,
    var y: Int,
    val speed: Int = 1,
    val isEdgeBonus: Boolean = false,
    val isGolden: Boolean = false
)
