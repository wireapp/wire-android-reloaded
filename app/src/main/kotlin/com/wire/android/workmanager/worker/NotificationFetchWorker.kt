package com.wire.android.workmanager.worker

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.WireNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltWorker
class NotificationFetchWorker
@AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wireNotificationManager: WireNotificationManager,
    private val notificationManager: NotificationManagerCompat
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val USER_ID_INPUT_DATA = "worker_user_id_input_data"
        const val TAG = "notification_fetch_worker"
    }

    override suspend fun doWork(): Result {
        try {
            inputData.getString(USER_ID_INPUT_DATA)?.let { userId ->
                appLogger.d("$TAG Starting the NotificationFetchWorker")
                wireNotificationManager.fetchAndShowNotificationsOnce(userId)
            }
        } catch (exception: Exception) {
            appLogger.d("$TAG NotificationFetchWorker failed with exception: $exception")
            return Result.failure()
        }

        appLogger.d("$TAG NotificationFetchWorker finished successfully")
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .setContentTitle(applicationContext.getString(R.string.label_fetching_your_messages))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        return ForegroundInfo(1, notification)
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.OTHER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MIN)
            .setName(NotificationConstants.OTHER_CHANNEL_NAME)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

}
