// Dice simulators

import kotlin.random.Random

val sidesOptions = setOf(4, 6, 8, 10, 12, 20)

fun dieRoll(sides: Int): Int {
    require(sides in sidesOptions) { "Invalid number of die sides: $sides" }
    return Random.nextInt(sides) + 1
}

fun diceRoll(numDice: Int, sides: Int): IntArray {
    require(numDice > 0) { "Invalid number of dice: $numDice" }
    return IntArray(numDice) { dieRoll(sides) }
}
