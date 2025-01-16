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

package com.wire.android.ui.home.conversations.messages

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayerProvider
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnResetSession
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogHelper
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.startFileShareIntent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.ObserveAssetStatusesUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageTransferStatusUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionResult
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import com.wire.kalium.logic.functional.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class ConversationMessagesViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val getMessageByIdUseCase: GetMessageByIdUseCase,
    private val updateAssetMessageDownloadStatus: UpdateAssetMessageTransferStatusUseCase,
    private val observeAssetStatusesUseCase: ObserveAssetStatusesUseCase,
    private val fileManager: FileManager,
    private val dispatchers: DispatcherProvider,
    private val getMessageForConversation: GetMessagesForConversationUseCase,
    private val toggleReaction: ToggleReactionUseCase,
    private val resetSession: ResetSessionUseCase,
    private val audioMessagePlayerProvider: ConversationAudioMessagePlayerProvider,
    private val getConversationUnreadEventsCount: GetConversationUnreadEventsCountUseCase,
    private val clearUsersTypingEvents: ClearUsersTypingEventsUseCase,
    private val getSearchedConversationMessagePosition: GetSearchedConversationMessagePositionUseCase,
    private val deleteMessage: DeleteMessageUseCase,
) : SavedStateViewModel(savedStateHandle) {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId
    private val searchedMessageIdNavArgs: String? = conversationNavArgs.searchedMessageId
    private val audioMessagePlayer = audioMessagePlayerProvider.provide()

    var conversationViewState by mutableStateOf(
        ConversationMessagesViewState(
            searchedMessageId = searchedMessageIdNavArgs
        )
    )
        private set

    var deleteMessageDialogsState: DeleteMessageDialogsState by mutableStateOf(
        DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    )
        private set

    val deleteMessageHelper = DeleteMessageDialogHelper(
        viewModelScope,
        conversationId,
        ::updateDeleteDialogState
    ) { messageId, deleteForEveryone, _ ->
        deleteMessage(
            conversationId = conversationId,
            messageId = messageId,
            deleteForEveryone = deleteForEveryone
        )
            .onFailure { onSnackbarMessage(ConversationSnackbarMessages.ErrorDeletingMessage) }
    }

    private var lastImageMessageShownOnGallery: UIMessage.Regular? = null
    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        clearOrphanedTypingEvents()
        loadPaginatedMessages()
        loadLastMessageInstant()
        observeAudioPlayerState()
        observeAssetStatuses()
    }

    val currentTimeInMillisFlow: Flow<Long> = flow {
        while (true) {
            delay(CURRENT_TIME_REFRESH_WINDOW_IN_MILLIS)
            emit(System.currentTimeMillis())
        }
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = CoroutineScope(Dispatchers.Default),
            replay = 1,
            started = SharingStarted.WhileSubscribed()
        )

    fun navigateToReplyOriginalMessage(message: UIMessage) {
        if (message.messageContent is UIMessageContent.TextMessage) {
            val originalMessageId =
                ((message.messageContent as UIMessageContent.TextMessage)
                    .messageBody.quotedMessage as UIQuotedMessage.UIQuotedData)
                    .messageId
            conversationViewState = conversationViewState.copy(
                searchedMessageId = originalMessageId
            )
            loadPaginatedMessages()
        }
    }

    private fun clearOrphanedTypingEvents() {
        viewModelScope.launch { clearUsersTypingEvents() }
    }

    private fun observeAudioPlayerState() {
        val observableAudioMessagesState = audioMessagePlayer.observableAudioMessagesState
            .map { audioMessageStates -> audioMessageStates.mapKeys { it.key.messageId } }

        viewModelScope.launch {
            combine(
                observableAudioMessagesState,
                audioMessagePlayer.audioSpeed,
                audioMessagePlayer.playingAudioMessageFlow
            ) { audioMessageStates, audioSpeed, playingAudiMessage ->
                AudioMessagesState(audioMessageStates.toPersistentMap(), audioSpeed, playingAudiMessage)
            }
                .collect { conversationViewState = conversationViewState.copy(audioMessagesState = it) }
        }
    }

    private fun observeAssetStatuses() {
        viewModelScope.launch {
            observeAssetStatusesUseCase(conversationId).collect {
                conversationViewState = conversationViewState.copy(
                    assetStatuses = it.toPersistentMap()
                )
            }
        }
    }

    private fun loadPaginatedMessages() = viewModelScope.launch {
        val lastReadIndex = conversationViewState.searchedMessageId?.let { messageId ->
            when (val result = getSearchedConversationMessagePosition(
                conversationId = conversationId,
                messageId = messageId
            )) {
                is GetSearchedConversationMessagePositionUseCase.Result.Success -> result.position
                is GetSearchedConversationMessagePositionUseCase.Result.Failure -> 0
            }
        } ?: when (val result = getConversationUnreadEventsCount(conversationId)) {
            is GetConversationUnreadEventsCountUseCase.Result.Success -> result.amount.toInt()
            is GetConversationUnreadEventsCountUseCase.Result.Failure -> 0
        }

        val paginatedMessagesFlow = getMessageForConversation(conversationId, lastReadIndex)
            .fetchAudioWavesMaskIfNeeded()
            .flowOn(dispatchers.io())

        conversationViewState = conversationViewState.copy(
            messages = paginatedMessagesFlow,
            firstUnreadEventIndex = max(lastReadIndex - 1, 0)
        )

        handleSelectedSearchedMessageHighlighting()
    }

    private suspend fun handleSelectedSearchedMessageHighlighting() {
        viewModelScope.launch {
            delay(3.seconds)
            conversationViewState = conversationViewState.copy(
                searchedMessageId = null
            )
        }
    }

    private fun loadLastMessageInstant() = viewModelScope.launch {
        observeConversationDetails(conversationId)
            .flowOn(dispatchers.io())
            .collect { conversationDetailsResult ->
                if (conversationDetailsResult is ObserveConversationDetailsUseCase.Result.Success) {
                    val lastUnreadInstant = conversationDetailsResult.conversationDetails.conversation.lastReadDate
                    conversationViewState = conversationViewState.copy(firstUnreadInstant = lastUnreadInstant)
                }
            }
    }

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    // This will download the asset remotely to an internal temporary storage or fetch it from the local database if it had been previously
    // downloaded. After doing so, a dialog is shown to ask the user whether he wants to open the file or download it to external storage
    fun downloadOrFetchAssetAndShowDialog(messageId: String) = viewModelScope.launch(dispatchers.io()) {
        attemptDownloadOfAsset(messageId)?.let { (messageId, bundle) ->
            showOnAssetDownloadedDialog(bundle, messageId)
        }
    }

    fun downloadAssetExternally(messageId: String) = viewModelScope.launch(dispatchers.io()) {
        attemptDownloadOfAsset(messageId)?.let { (messageId, bundle) ->
            onSaveFile(bundle.fileName, bundle.dataPath, bundle.dataSize, messageId)
        }
    }

    fun downloadAndOpenAsset(messageId: String) = viewModelScope.launch(dispatchers.io()) {
        attemptDownloadOfAsset(messageId)?.let { (_, bundle) ->
            onOpenFileWithExternalApp(bundle.dataPath, bundle.fileName)
        }
    }

    private suspend fun attemptDownloadOfAsset(messageId: String): Pair<String, AssetBundle>? {
        val messageDataResult = getMessageByIdUseCase(conversationId, messageId)
        return when {
            messageDataResult !is GetMessageByIdUseCase.Result.Success -> {
                appLogger.w("Failed when fetching details of message to download asset: $messageDataResult")
                null
            }

            messageDataResult.message.content !is MessageContent.Asset -> {
                // This _should_ not even happen, tho. Unless UI is buggy. So... do we crash?! Better not.
                appLogger.w("Attempting to download assets of a non-asset message. Ignoring user input.")
                null
            }

            else -> try {
                val messageContent = messageDataResult.message.content as MessageContent.Asset
                val assetContent = messageContent.value
                assetDataPath(conversationId, messageId)?.let { (path, _) ->
                    messageId to AssetBundle(
                        key = assetContent.remoteData.assetId,
                        dataPath = path,
                        fileName = assetContent.name ?: DEFAULT_ASSET_NAME,
                        dataSize = assetContent.sizeInBytes,
                        mimeType = assetContent.mimeType,
                        assetType = AttachmentType.fromMimeTypeString(assetContent.mimeType)
                    )
                }
            } catch (e: OutOfMemoryError) {
                appLogger.e("There was an OutOfMemory error while downloading the asset")
                onSnackbarMessage(ConversationSnackbarMessages.ErrorDownloadingAsset)
                updateAssetMessageDownloadStatus(AssetTransferStatus.FAILED_DOWNLOAD, conversationId, messageId)
                null
            }
        }
    }

    private fun onOpenFileWithExternalApp(assetDataPath: Path, assetName: String?) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                fileManager.openWithExternalApp(assetDataPath, assetName) { onOpenFileError() }
                hideOnAssetDownloadedDialog()
            }
        }
    }

    private fun onSaveFile(assetName: String, assetDataPath: Path, assetSize: Long, messageId: String) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                fileManager.saveToExternalStorage(assetName, assetDataPath, assetSize) { savedFileName: String? ->
                    updateAssetMessageDownloadStatus(AssetTransferStatus.SAVED_EXTERNALLY, conversationId, messageId)
                    onFileSavedToExternalStorage(savedFileName)
                    hideOnAssetDownloadedDialog()
                }
            }
        }
    }

    fun showOnAssetDownloadedDialog(assetBundle: AssetBundle, messageId: String) {
        conversationViewState = conversationViewState.copy(
            downloadedAssetDialogState = DownloadedAssetDialogVisibilityState.Displayed(assetBundle, messageId)
        )
    }

    fun hideOnAssetDownloadedDialog() {
        conversationViewState = conversationViewState.copy(downloadedAssetDialogState = DownloadedAssetDialogVisibilityState.Hidden)
    }

    fun toggleReaction(messageId: String, reactionEmoji: String) {
        viewModelScope.launch {
            toggleReaction(conversationId, messageId, reactionEmoji)
        }
    }

    fun onResetSession(userId: UserId, clientId: String?) {
        viewModelScope.launch {
            when (resetSession(conversationId, userId, ClientId(clientId.orEmpty()))) {
                is ResetSessionResult.Failure -> {
                    onSnackbarMessage(OnResetSession(UIText.StringResource(R.string.label_general_error)))
                }

                is ResetSessionResult.Success -> {
                    onSnackbarMessage(OnResetSession(UIText.StringResource(R.string.label_reset_session_success)))
                }
            }
        }
    }

    fun shareAsset(context: Context, messageId: String) {
        viewModelScope.launch {
            assetDataPath(conversationId, messageId)?.run {
                context.startFileShareIntent(first, second)
            }
        }
    }

    private suspend fun assetDataPath(conversationId: QualifiedID, messageId: String): Pair<Path, String>? =
        getMessageAsset(conversationId, messageId).await().run {
            return when (this) {
                is MessageAssetResult.Success -> decodedAssetPath to assetName
                else -> null
            }
        }

    private fun onOpenFileError() {
        onSnackbarMessage(ConversationSnackbarMessages.ErrorOpeningAssetFile)
    }

    private fun onFileSavedToExternalStorage(assetName: String?) {
        onSnackbarMessage(ConversationSnackbarMessages.OnFileDownloaded(assetName))
    }

    private fun updateDeleteDialogState(newValue: (DeleteMessageDialogsState.States) -> DeleteMessageDialogsState) =
        (deleteMessageDialogsState as? DeleteMessageDialogsState.States)?.let {
            deleteMessageDialogsState = newValue(it)
        }

    fun audioClick(messageId: String) {
        viewModelScope.launch {
            audioMessagePlayer.playAudio(conversationId, messageId)
        }
    }

    fun changeAudioPosition(messageId: String, position: Int) {
        viewModelScope.launch {
            audioMessagePlayer.setPosition(conversationId, messageId, position)
        }
    }

    fun changeAudioSpeed(audioSpeed: AudioSpeed) {
        viewModelScope.launch {
            audioMessagePlayer.setSpeed(audioSpeed)
        }
    }

    fun updateImageOnFullscreenMode(message: UIMessage.Regular?) {
        lastImageMessageShownOnGallery = message
    }

    fun getAndResetLastFullscreenMessage(messageId: String): UIMessage.Regular? {
        lastImageMessageShownOnGallery?.let { onFullscreenMessage ->
            // We need to reset the lastImageMessageShownOnGallery as we are already handling it here
            updateImageOnFullscreenMode(null)
            // This condition should always be true, but we check it just in case the lastImageMessageShownOnGallery
            // is not the same one that we are replying to
            if (onFullscreenMessage.header.messageId == messageId) {
                return onFullscreenMessage
            }
        }
        return null
    }

    fun showDeleteMessageDialog(messageId: String, deleteForEveryone: Boolean) =
        if (deleteForEveryone) {
            updateDeleteDialogState {
                it.copy(
                    forEveryone = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId
                    )
                )
            }
        } else {
            updateDeleteDialogState {
                it.copy(
                    forYourself = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId
                    )
                )
            }
        }

    // checking all the new messages if it's an AudioMessage and fetch WavesMask for it if so
    private fun Flow<PagingData<UIMessage>>.fetchAudioWavesMaskIfNeeded(): Flow<PagingData<UIMessage>> =
        map {
            it.map { message ->
                if (message.messageContent is UIMessageContent.AudioAssetMessage) {
                    viewModelScope.launch {
                        audioMessagePlayer.fetchWavesMask(conversationId, message.header.messageId)
                    }
                }
                message
            }
        }

    private companion object {
        const val DEFAULT_ASSET_NAME = "Wire File"
        const val CURRENT_TIME_REFRESH_WINDOW_IN_MILLIS: Long = 60_000
    }
}
