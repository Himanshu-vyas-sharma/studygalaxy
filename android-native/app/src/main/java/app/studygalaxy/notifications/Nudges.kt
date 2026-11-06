package app.studygalaxy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.studygalaxy.MainActivity
import app.studygalaxy.data.api.ApiModule
import app.studygalaxy.data.local.TokenStore
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val CHANNEL_FRIENDS = "friends_focusing"
const val CHANNEL_IDLE = "idle_nudge"

private const val NOTIF_FRIENDS = 51
private const val NOTIF_IDLE = 52

private const val WORK_FRIENDS = "sg_friend_watch"
private const val WORK_IDLE = "sg_idle_nudge"

private const val PREFS = "sg_nudges"
private const val KEY_LAST_ACTIVE = "lastActiveAt"
private const val KEY_LAST_FRIEND_NUDGE = "lastFriendNudgeAt"
private const val KEY_LAST_IDLE_NUDGE = "lastIdleNudgeAt"

/** Don't wake a student at 3am. Nudges are suppressed inside these hours. */
private const val QUIET_START_HOUR = 22
private const val QUIET_END_HOUR = 7

/** At most one "friend is studying" nudge per this window. */
private val FRIEND_NUDGE_COOLDOWN_MS = TimeUnit.HOURS.toMillis(2)

/** How long the app must go unused before the idle nudge fires. */
val IDLE_AFTER_MS: Long = TimeUnit.MINUTES.toMillis(150) // 2.5 hours

object Nudges {

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Called whenever the user opens the app or finishes a session. */
    fun markActive(ctx: Context) {
        prefs(ctx).edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    fun lastActive(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_ACTIVE, 0L)

    fun inQuietHours(now: Long = System.currentTimeMillis()): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_START_HOUR || hour < QUIET_END_HOUR
    }

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_FRIENDS, "Friends studying", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "When someone in your circles starts a focus session." }
        )
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_IDLE, "Study nudges", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "A reminder when you haven't focused in a while." }
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun show(ctx: Context, channel: String, id: Int, title: String, body: String) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.star_on)
            .setPriority(
                if (channel == CHANNEL_FRIENDS) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_LOW
            )
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        runCatching {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n)
        }
    }

    /**
     * Starts both background loops. Safe to call on every launch — KEEP means an
     * already-scheduled loop is left alone rather than restarted.
     *
     * 15 minutes is Android's hard floor for periodic work, so a friend nudge can
     * lag a friend's start by up to that much. Real-time delivery needs FCM push.
     */
    fun scheduleAll(ctx: Context) {
        ensureChannels(ctx)
        markActive(ctx)
        val wm = WorkManager.getInstance(ctx)

        wm.enqueueUniquePeriodicWork(
            WORK_FRIENDS,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FriendWatchWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
        )

        // Checked every 30 min; the worker itself decides whether 2.5 h of
        // inactivity have actually passed.
        wm.enqueueUniquePeriodicWork(
            WORK_IDLE,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<IdleNudgeWorker>(30, TimeUnit.MINUTES).build()
        )
    }

    fun cancelAll(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork(WORK_FRIENDS)
        wm.cancelUniqueWork(WORK_IDLE)
    }

    internal fun canNudgeFriends(ctx: Context): Boolean {
        val last = prefs(ctx).getLong(KEY_LAST_FRIEND_NUDGE, 0L)
        return System.currentTimeMillis() - last >= FRIEND_NUDGE_COOLDOWN_MS
    }

    internal fun recordFriendNudge(ctx: Context) {
        prefs(ctx).edit().putLong(KEY_LAST_FRIEND_NUDGE, System.currentTimeMillis()).apply()
    }

    internal fun lastIdleNudge(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_IDLE_NUDGE, 0L)

    internal fun recordIdleNudge(ctx: Context) {
        prefs(ctx).edit().putLong(KEY_LAST_IDLE_NUDGE, System.currentTimeMillis()).apply()
    }
}

/**
 * "Your friend is studying — what are you doing?"
 * Stays silent when the user is already in a session, when nobody is studying,
 * during quiet hours, or inside the cooldown.
 */
class FriendWatchWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (TokenStore.getJwt(ctx) == null) return Result.success()
        if (Nudges.inQuietHours() || !Nudges.canNudgeFriends(ctx)) return Result.success()

        val reply = runCatching { ApiModule.get(ctx).friendsFocusing() }.getOrNull()
            ?: return Result.retry()

        // Don't interrupt someone who is already focusing — that is the goal state.
        if (reply.youAreFocusing || reply.friends.isEmpty()) return Result.success()

        val names = reply.friends.map { it.cosmicName }
        val (title, body) = Motivation.friendStudying(names, reply.friends.firstOrNull()?.intention)
        Nudges.show(ctx, CHANNEL_FRIENDS, NOTIF_FRIENDS, title, body)
        Nudges.recordFriendNudge(ctx)
        // A friend's session also counts as activity worth not nagging over.
        Nudges.recordIdleNudge(ctx)
        return Result.success()
    }
}

/** Fires once per 2.5 hours of not opening the app, outside quiet hours. */
class IdleNudgeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (TokenStore.getJwt(ctx) == null) return Result.success()
        if (Nudges.inQuietHours()) return Result.success()

        val now = System.currentTimeMillis()
        val lastActive = Nudges.lastActive(ctx)
        if (lastActive == 0L) return Result.success()          // never opened yet
        if (now - lastActive < IDLE_AFTER_MS) return Result.success()
        if (now - Nudges.lastIdleNudge(ctx) < IDLE_AFTER_MS) return Result.success()

        // A streak worth protecting makes a much stronger nudge than a generic one.
        val streak = runCatching { ApiModule.get(ctx).me().streakDays }.getOrDefault(0)
        val (title, body) = Motivation.idleNudge(streak)
        Nudges.show(ctx, CHANNEL_IDLE, NOTIF_IDLE, title, body)
        Nudges.recordIdleNudge(ctx)
        return Result.success()
    }
}
