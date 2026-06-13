package hr.ferit.brunodidovic.swish.repository

import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.brunodidovic.swish.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun getUserById(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)?.copy(id = snapshot.id)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateNotifiedAchievements(
        userId: String,
        achievements: List<String>
    ): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("notifiedAchievements", achievements)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}