package hr.ferit.brunodidovic.swish.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hr.ferit.brunodidovic.swish.model.Workout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val workoutsCollection = firestore.collection("workouts")

    fun getWorkoutsForUser(userId: String): Flow<List<Workout>> = callbackFlow {
        val listener = workoutsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val workouts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Workout::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(workouts)
            }
        awaitClose { listener.remove() }
    }

    fun getWorkoutById(workoutId: String): Flow<Workout?> = callbackFlow {
        val listener = workoutsCollection
            .document(workoutId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val workout = snapshot?.toObject(Workout::class.java)?.copy(id = snapshot.id)
                trySend(workout)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveWorkout(workout: Workout): Result<String> {
        return try {
            val docRef = if (workout.id.isEmpty()) {
                workoutsCollection.add(workout).await()
            } else {
                workoutsCollection.document(workout.id).set(workout).await()
                workoutsCollection.document(workout.id)
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}