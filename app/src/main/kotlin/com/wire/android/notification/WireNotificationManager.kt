package com.wire.android.notification

import com.wire.android.di.GetIncomingCallsUseCaseProvider
import com.wire.android.di.GetNotificationsUseCaseProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val getNotificationProvider: GetNotificationsUseCaseProvider.Factory,
    private val getIncomingCallsProvider: GetIncomingCallsUseCaseProvider.Factory,
    private val messagesManager: MessageNotificationManager,
    private val callsManager: CallNotificationManager,
) {

    /**
     * Sync all the Pending events, fetch Message notifications from DB once and show it.
     * Can be used in Services (e.g., after receiving FCM)
     * @param userId QualifiedID of User that need to check Notifications for
     */
    suspend fun fetchAndShowMessageNotificationsOnce(userId: UserId) {
        coreLogic.getSessionScope(userId).syncPendingEvents()

        val notificationsList = getNotificationProvider.create(userId)
            .getNotifications()
            .first()

        messagesManager.handleNotification(listOf(), notificationsList, userId)

        val callsList = getIncomingCallsProvider.create(userId)
            .getCalls()
            .first()

        callsManager.handleCalls(callsList, userId)
    }

    /**
     * Infinitely listen for the new Message notifications and show it.
     * Can be used for listening for the Notifications when the app is running.
     * @param userIdFlow Flow of QualifiedID of User
     */
    @ExperimentalCoroutinesApi
    suspend fun observeMessageNotifications(userIdFlow: Flow<UserId?>) {
        userIdFlow
            .flatMapLatest { userId ->
                if (userId != null) {
                    getNotificationProvider.create(userId).getNotifications()
                } else {
                    // if userId == null means there is no current user (e.g., logged out)
                    // so we need to unsubscribe from the notification changes (it's done by `flatMapLatest`)
                    // and remove the notifications that were displayed previously
                    // (empty list in here makes all the pre. notifications be removed)
                    flowOf(listOf())
                }
                    // we need to remember prev. displayed Notifications,
                    // so we can remove notifications that were displayed previously but are not in the new list
                    .scan((listOf<LocalNotificationConversation>() to listOf<LocalNotificationConversation>()))
                    { old, newList -> old.second to newList }
                    // combining all the data that is necessary for Notifications into small data class,
                    // just to make it more readable than
                    // Triple<List<LocalNotificationConversation>, List<LocalNotificationConversation>, QualifiedID?>
                    .map { (oldNotifications, newNotifications) ->
                        MessagesNotificationsData(oldNotifications, newNotifications, userId)
                    }
            }
            .collect { (oldNotifications, newNotifications, userId) ->
                messagesManager.handleNotification(oldNotifications, newNotifications, userId)
            }
    }

    private data class MessagesNotificationsData(
        val oldNotifications: List<LocalNotificationConversation>,
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )
}
