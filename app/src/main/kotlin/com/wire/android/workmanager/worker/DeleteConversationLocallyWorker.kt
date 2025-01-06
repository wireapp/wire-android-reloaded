/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.workmanager.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.functional.fold
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * A worker responsible for performing local deletion of conversations in the background.
 *
 * This worker is used to delete conversations along with their associated assets. Since the
 * deletion process can involve large amounts of data (e.g., clearing files, database entries),
 * it is executed as a background task to avoid blocking the main thread or user interactions.
 *
 * @param appContext The application context, provided by WorkManager.
 * @param workerParams Parameters associated with this work request, including input data.
 * @param coreLogic A utility object that handles core application logic, such as session and
 * conversation management.
 */
@HiltWorker
class DeleteConversationLocallyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coreLogic: CoreLogic
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        inputData.getString(CONVERSATION_ID)?.let { id ->
            val conversationId = QualifiedIdMapperImpl(null).fromStringToQualifiedID(id)
            val currentSession = coreLogic.getGlobalScope().session.currentSession()
            if (currentSession is CurrentSessionResult.Success && currentSession.accountInfo.isValid()) {
                val deleteConversationLocally =
                    coreLogic.getSessionScope(currentSession.accountInfo.userId).conversations.deleteConversationLocallyUseCase
                deleteConversationLocally(conversationId)
                    .fold({ Result.failure() }, { Result.success() })
            } else {
                Result.failure()
            }
        } ?: Result.failure()
    }

    companion object {
        private const val NAME = "delete_conversation_locally_"
        const val CONVERSATION_ID = "delete_conversation_locally_conversation_id"

        fun createUniqueWorkName(conversationId: String): String {
            return "$NAME$conversationId"
        }
    }
}

enum class ConversationDeletionLocallyStatus {
    IDLE, RUNNING, SUCCEEDED, FAILED
}

/**
 * Enqueues a background task to delete a conversation and its associated assets locally.
 *
 * This function uses the WorkManager API to create and enqueue a one-time work request for
 * the `DeleteConversationLocallyWorker`. The work request is configured to run either as
 * expedited (if quota allows) or as non-expedited work. If a deletion task for the same
 * conversation is already running, the existing task will be kept or replaced based on the
 * work policy.
 *
 * Additionally, this function returns a [Flow] that allows observing the status of the
 * conversation deletion process.
 *
 * *Not having a Backoff criteria is intentional.* If there is an error during the process,
 * it will be displayed to the users immediately rather than retrying silently in the background.
 */
fun WorkManager.enqueueConversationDeletionLocally(conversationId: ConversationId): Flow<ConversationDeletionLocallyStatus> {
    val workName = DeleteConversationLocallyWorker.createUniqueWorkName(conversationId.toString())
    val request = OneTimeWorkRequestBuilder<DeleteConversationLocallyWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setInputData(workDataOf(DeleteConversationLocallyWorker.CONVERSATION_ID to conversationId.toString()))
        .build()
    val isAlreadyRunning = getWorkInfosForUniqueWorkLiveData(workName)
        .value
        ?.firstOrNull()
        ?.state == WorkInfo.State.RUNNING
    enqueueUniqueWork(
        workName,
        if (isAlreadyRunning) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
        request
    )

    return observeConversationDeletionStatusLocally(conversationId)
}

/**
 * Observes the status of a conversation deletion task running locally.
 *
 * This function returns a [Flow] that emits the current status of the conversation deletion
 * process for the given conversation ID. The status is determined by monitoring the
 * [WorkInfo] associated with the unique work name of the `DeleteConversationLocallyWorker`.
 *
 * The returned statuses include:
 * - [ConversationDeletionLocallyStatus.RUNNING]: The deletion task is currently in progress.
 * - [ConversationDeletionLocallyStatus.SUCCEEDED]: The deletion task completed successfully.
 * - [ConversationDeletionLocallyStatus.FAILED]: The deletion task failed, was blocked, or cancelled.
 * - [ConversationDeletionLocallyStatus.IDLE]: No active work is found for the given conversation ID.
 */
fun WorkManager.observeConversationDeletionStatusLocally(conversationId: ConversationId): Flow<ConversationDeletionLocallyStatus> {
    return getWorkInfosForUniqueWorkFlow(DeleteConversationLocallyWorker.createUniqueWorkName(conversationId.toString()))
        .mapNotNull { workInfos ->
            workInfos.lastOrNull()?.let { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING -> ConversationDeletionLocallyStatus.RUNNING

                    WorkInfo.State.SUCCEEDED -> ConversationDeletionLocallyStatus.SUCCEEDED
                    WorkInfo.State.FAILED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.CANCELLED -> ConversationDeletionLocallyStatus.FAILED
                }
            } ?: ConversationDeletionLocallyStatus.IDLE
        }
}
