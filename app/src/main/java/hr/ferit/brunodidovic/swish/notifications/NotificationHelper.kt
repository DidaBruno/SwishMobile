package hr.ferit.brunodidovic.swish.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import hr.ferit.brunodidovic.swish.R

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = NotificationManagerCompat.from(context)

        val reminders = NotificationChannelCompat.Builder(
            CHANNEL_REMINDERS, NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Workout reminders")
            .setDescription("Daily reminders to work out and keep your streak")
            .build()

        val achievements = NotificationChannelCompat.Builder(
            CHANNEL_ACHIEVEMENTS, NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName("Achievements")
            .setDescription("Alerts when you reach a milestone")
            .build()

        manager.createNotificationChannel(reminders)
        manager.createNotificationChannel(achievements)
    }

    fun showWorkoutReminder() = post(
        channelId = CHANNEL_REMINDERS,
        id = NOTIF_WORKOUT_TODAY,
        iconRes = R.drawable.ic_swish_basketball,
        title = "⛹️ No workout logged today",
        message = "Get a session in to keep the momentum going."
    )

    fun showStreakReminder(streakDays: Int) = post(
        channelId = CHANNEL_REMINDERS,
        id = NOTIF_STREAK,
        iconRes = R.drawable.ic_swish_basketball,
        title = "💪 Keep your $streakDays-day streak alive",
        message = "Log a workout today so you don't break it."
    )

    fun showAchievement(id: Int, title: String, message: String) = post(
        channelId = CHANNEL_ACHIEVEMENTS,
        id = id,
        iconRes = R.drawable.ic_swish_basketball,
        title = title,
        message = message
    )

    @SuppressLint("MissingPermission")
    private fun post(channelId: String, id: Int, iconRes: Int, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_ACHIEVEMENTS = "achievements"
        const val NOTIF_WORKOUT_TODAY = 1001
        const val NOTIF_STREAK = 1002
    }
}