package hr.ferit.brunodidovic.swish.repository

import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.brunodidovic.swish.model.DrillTemplate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val templatesCollection = firestore.collection("drillTemplates")

    fun getTemplatesForUser(userId: String): Flow<List<DrillTemplate>> = callbackFlow {
        val listener = templatesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val templates = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DrillTemplate::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(templates)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveTemplate(template: DrillTemplate): Result<String> {
        return try {
            val docRef = templatesCollection.add(template).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> {
        return try {
            templatesCollection.document(templateId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}