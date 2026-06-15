package hr.ferit.brunodidovic.swish.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import hr.ferit.brunodidovic.swish.domain.StatsCalculator
import hr.ferit.brunodidovic.swish.notifications.NotificationHelper
import hr.ferit.brunodidovic.swish.repository.AuthRepository
import hr.ferit.brunodidovic.swish.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // nothing to do if no one is logged in
            val uid = authRepository.currentUser?.uid ?: return Result.success()

            val workouts = workoutRepository.getWorkoutsForUser(uid).first()
            val today = LocalDate.now()

            if (!StatsCalculator.hasWorkoutOn(workouts, today)) {
                notificationHelper.showWorkoutReminder()

                val streak = StatsCalculator.currentStreak(workouts, today)
                if (streak >= 2) {
                    notificationHelper.showStreakReminder(streak)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}