package hr.ferit.brunodidovic.swish.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
    val notifiedAchievements: List<String> = emptyList()
)