package hr.ferit.brunodidovic.swish.domain

import hr.ferit.brunodidovic.swish.model.Workout
import java.time.LocalDate
import java.util.SortedSet

// lifetime totals computed from all workouts
data class ProfileStats(
    val workouts: Int = 0,
    val longestStreak: Int = 0,
    val twos: Int = 0,
    val threes: Int = 0,
    val freeThrows: Int = 0,
    val layups: Int = 0,
    val dunks: Int = 0,
    val totalShots: Int = 0,
    val gamesPlayed: Int = 0
)

// static milestone definitions. IDs are HARD-CODED and must never change
// (notifiedAchievements stores them to avoid re-notifying).
data class MilestoneDef(
    val id: String,
    val family: String,
    val threshold: Int,
    val valueOf: (ProfileStats) -> Int
)

val MILESTONES = listOf(
    MilestoneDef("workouts_100",  "Workouts logged",  100)   { it.workouts },
    MilestoneDef("workouts_500",  "Workouts logged",  500)   { it.workouts },
    MilestoneDef("workouts_1000", "Workouts logged",  1000)  { it.workouts },
    MilestoneDef("streak_10",     "Day streak",       10)    { it.longestStreak },
    MilestoneDef("streak_50",     "Day streak",       50)    { it.longestStreak },
    MilestoneDef("streak_100",    "Day streak",       100)   { it.longestStreak },
    MilestoneDef("twos_100",      "Twos made",        100)   { it.twos },
    MilestoneDef("twos_1000",     "Twos made",        1000)  { it.twos },
    MilestoneDef("twos_10000",    "Twos made",        10000) { it.twos },
    MilestoneDef("threes_100",    "Threes made",      100)   { it.threes },
    MilestoneDef("threes_1000",   "Threes made",      1000)  { it.threes },
    MilestoneDef("threes_10000",  "Threes made",      10000) { it.threes },
    MilestoneDef("ft_100",        "Free throws made", 100)   { it.freeThrows },
    MilestoneDef("ft_1000",       "Free throws made", 1000)  { it.freeThrows },
    MilestoneDef("ft_10000",      "Free throws made", 10000) { it.freeThrows },
    MilestoneDef("layups_100",    "Layups made",      100)   { it.layups },
    MilestoneDef("layups_1000",   "Layups made",      1000)  { it.layups },
    MilestoneDef("layups_10000",  "Layups made",      10000) { it.layups },
    MilestoneDef("dunks_10",      "Dunks made",       10)    { it.dunks },
    MilestoneDef("dunks_100",     "Dunks made",       100)   { it.dunks },
    MilestoneDef("dunks_1000",    "Dunks made",       1000)  { it.dunks },
    MilestoneDef("shots_1000",    "Total shots made", 1000)  { it.totalShots },
    MilestoneDef("shots_10000",   "Total shots made", 10000) { it.totalShots },
    MilestoneDef("shots_50000",   "Total shots made", 50000) { it.totalShots },
    MilestoneDef("games_10",      "Games played",     10)    { it.gamesPlayed },
    MilestoneDef("games_50",      "Games played",     50)    { it.gamesPlayed },
    MilestoneDef("games_100",     "Games played",     100)   { it.gamesPlayed }
)

object StatsCalculator {

    fun computeStats(workouts: List<Workout>): ProfileStats {
        val twos = workouts.sumOf { it.drills.shooting.twos }
        val threes = workouts.sumOf { it.drills.shooting.threes }
        val freeThrows = workouts.sumOf { it.drills.shooting.freeThrows }
        val layups = workouts.sumOf { it.drills.shooting.layups }
        val dunks = workouts.sumOf { it.drills.shooting.dunks }
        return ProfileStats(
            workouts = workouts.size,
            longestStreak = longestStreak(workouts),
            twos = twos,
            threes = threes,
            freeThrows = freeThrows,
            layups = layups,
            dunks = dunks,
            totalShots = twos + threes + freeThrows + layups + dunks,
            gamesPlayed = workouts.sumOf { it.games.size }
        )
    }

    // longest run of consecutive days ever — used for achievements
    fun longestStreak(workouts: List<Workout>): Int {
        val days = workoutDays(workouts)
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (day in days) {
            run = if (prev != null && prev.plusDays(1) == day) run + 1 else 1
            if (run > longest) longest = run
            prev = day
        }
        return longest
    }

    // active streak ending today (or yesterday if today is empty) — used for reminders
    fun currentStreak(workouts: List<Workout>, today: LocalDate): Int {
        val days = workoutDays(workouts)
        val anchor = if (days.contains(today)) today else today.minusDays(1)
        if (!days.contains(anchor)) return 0
        var count = 0
        var d = anchor
        while (days.contains(d)) {
            count++
            d = d.minusDays(1)
        }
        return count
    }

    fun hasWorkoutOn(workouts: List<Workout>, date: LocalDate): Boolean =
        workoutDays(workouts).contains(date)

    // which milestone IDs the stats have crossed — used for achievement alerts
    fun reachedMilestoneIds(stats: ProfileStats): Set<String> =
        MILESTONES.filter { it.valueOf(stats) >= it.threshold }.map { it.id }.toSet()

    private fun workoutDays(workouts: List<Workout>): SortedSet<LocalDate> =
        workouts
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSortedSet()
}