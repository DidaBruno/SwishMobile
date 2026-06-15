package hr.ferit.brunodidovic.swish.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import hr.ferit.brunodidovic.swish.domain.MILESTONES
import hr.ferit.brunodidovic.swish.domain.StatsCalculator
import hr.ferit.brunodidovic.swish.notifications.NotificationHelper
import hr.ferit.brunodidovic.swish.repository.AuthRepository
import hr.ferit.brunodidovic.swish.repository.UserRepository
import hr.ferit.brunodidovic.swish.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

@HiltWorker
class AchievementWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val userRepository: UserRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val uid = authRepository.currentUser?.uid ?: return Result.success()

            val workouts = workoutRepository.getWorkoutsForUser(uid).first()
            val user = userRepository.getUserById(uid).first()

            val stats = StatsCalculator.computeStats(workouts)
            val reached = StatsCalculator.reachedMilestoneIds(stats)
            val alreadyNotified = user?.notifiedAchievements?.toSet() ?: emptySet()

            val newlyReached = reached - alreadyNotified
            if (newlyReached.isEmpty()) return Result.success()

            newlyReached.forEach { id ->
                val def = MILESTONES.firstOrNull { it.id == id } ?: return@forEach
                notificationHelper.showAchievement(
                    id = id.hashCode(),
                    title = "🏆 Milestone reached 🏆",
                    message = "${def.family}: ${formatThousands(def.threshold)}"
                )
            }

            // remember them so they never notify again
            userRepository.updateNotifiedAchievements(
                uid,
                (alreadyNotified + newlyReached).toList()
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun formatThousands(value: Int): String = "%,d".format(value)
}