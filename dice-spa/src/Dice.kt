// Dice simulator

import kotlin.random.Random

enum class Die(val sides: Int) {
    Four(4),
    Six(6),
    Eight(8),
    Ten(10),
    Twelve(12),
    Twenty(20);

    companion object {
        fun fromString(name: String) = when (name) {
            "d4" -> Four
            "d6" -> Six
            "d8" -> Eight
            "d10" -> Ten
            "d12" -> Twelve
            "d20" -> Twenty
            else -> throw IllegalArgumentException("invalid die: $name")
        }
    }

    override fun toString() = "d$sides"

    fun roll() = Random.nextInt(sides) + 1
}

val dieOptions = Die.entries.map { it.toString() }

fun diceRoll(numDice: Int, dieName: String): IntArray {
    require(numDice > 0) { "Invalid number of dice: $numDice" }
    val die = Die.fromString(dieName)
    return IntArray(numDice) { die.roll() }
}
