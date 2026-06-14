package hr.ferit.brunodidovic.swish.model

import com.google.firebase.firestore.Exclude

data class ShootingDrills(
    val twos: Int = 0,
    val threes: Int = 0,
    val freeThrows: Int = 0,
    val layups: Int = 0,
    val dunks: Int = 0
)

data class HandlingDrill(
    val name: String = "",
    val count: Int = 0,
    val unit: String = ""
)

data class TeamInfo(
    val players: String = "",
    val score: Int = 0
)

data class Game(
    val playersPerTeam: Int = 0,
    val teamA: TeamInfo = TeamInfo(),
    val teamB: TeamInfo = TeamInfo()
)

data class Drills(
    val shooting: ShootingDrills = ShootingDrills(),
    val handling: List<HandlingDrill> = emptyList()
)

data class Workout(
    @get:Exclude val id: String = "",
    val userId: String = "",
    val date: String = "",
    val createdAt: String = "",
    val drills: Drills = Drills(),
    val games: List<Game> = emptyList()
)