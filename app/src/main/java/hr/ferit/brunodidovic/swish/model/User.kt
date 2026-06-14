package hr.ferit.brunodidovic.swish.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class User(
    @get:Exclude val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val notifiedAchievements: List<String> = emptyList()
)