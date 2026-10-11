package app.studygalaxy.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

private const val CHANNEL_ID = "study_reminder"
private const val NOTIFICATION_ID = 42
private const val REQUEST_CODE = 1001

object StudyReminder {

    private fun hourFor(slot: String?): Int = when (slot) {
        "morning" -> 7
        "afternoon" -> 14
        "evening" -> 18
        "night" -> 21
        else -> 19
    }

    fun scheduleDaily(ctx: Context, slot: String?) {
        ensureChannel(ctx)
        val hour = hourFor(slot)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, StudyReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Inexact repeating is fine for a daily study nudge (no exact-alarm permission needed).
        alarmMgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun fire(ctx: Context) {
        ensureChannel(ctx)
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle("Your galaxy is waiting")
            .setContentText("Time for a focus session. Even 25 minutes grows a new star.")
            .setSmallIcon(android.R.drawable.star_on)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Daily Study Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle nudge at your chosen study time."
            }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        StudyReminder.fire(context)
    }
}
