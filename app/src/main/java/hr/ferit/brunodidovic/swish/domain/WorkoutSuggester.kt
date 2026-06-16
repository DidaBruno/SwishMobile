package hr.ferit.brunodidovic.swish.domain

data class ShootingSuggestion(
    val twos: Int,
    val threes: Int,
    val freeThrows: Int,
    val layups: Int,
    val dunks: Int
) {
    val total: Int get() = twos + threes + freeThrows + layups + dunks
}

object WorkoutSuggester {

    private const val STEP = 10 // values come in increments of 10
    private const val MAX_TOTAL = 120 // never suggest more than 120 shots
    private const val CATEGORIES = 5

    fun random(): ShootingSuggestion {
        // work in units of 10, each category get 1 unit to start (min 10 each),
        // then hand out a random number of extra units without exceeding the cap.
        val units = IntArray(CATEGORIES) { 1 }
        val maxExtraUnits = MAX_TOTAL / STEP - CATEGORIES   // = 7
        val extra = (0..maxExtraUnits).random()
        repeat(extra) {
            units[(0 until CATEGORIES).random()]++
        }
        return ShootingSuggestion(
            twos = units[0] * STEP,
            threes = units[1] * STEP,
            freeThrows = units[2] * STEP,
            layups = units[3] * STEP,
            dunks = units[4] * STEP
        )
    }
}