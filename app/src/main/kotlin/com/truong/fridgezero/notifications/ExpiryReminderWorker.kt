package com.truong.fridgezero.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truong.fridgezero.MainActivity
import com.truong.fridgezero.R
import com.truong.fridgezero.data.FridgeDb
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.freshnessOf
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ExpiryReminderWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val activeItems = FridgeDb.get(applicationContext)
      .foodItemDao()
      .getByStatusSnapshot(ItemStatus.ACTIVE)

    val today = LocalDate.now().toEpochDay()
    val expiringCount = activeItems.count { item ->
      val daysLeft = (item.expiresAtEpochDay - today).toInt()
      freshnessOf(daysLeft) != Freshness.OK
    }

    if (expiringCount > 0) {
      postNotification(expiringCount)
    }
    return Result.success()
  }

  private fun postNotification(expiringCount: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    ensureChannel()

    val intent = Intent(applicationContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      applicationContext,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(applicationContext.getString(R.string.notification_title))
      .setContentText(applicationContext.getString(R.string.notification_body_fmt, expiringCount))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
  }

  private fun ensureChannel() {
    val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return
    val channel = NotificationChannel(
      CHANNEL_ID,
      applicationContext.getString(R.string.notification_channel_name),
      NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val CHANNEL_ID = "fridgezero_expiry"
    private const val NOTIFICATION_ID = 1001
  }
}

object ExpiryReminderScheduler {
  private const val WORK_NAME = "fridgezero_daily_expiry"

  fun scheduleDaily(context: Context) {
    val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
      WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request
    )
  }

  fun cancel(context: Context) {
    WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
  }
}
