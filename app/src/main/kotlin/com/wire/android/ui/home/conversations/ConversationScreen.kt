package com.wire.android.ui.home.conversations

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dialogs.OngoingActiveCallDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarBaseViewModel
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDeletingMessage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDownloadingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorOpeningAssetFile
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorPickingAttachment
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingImage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
import com.wire.android.ui.home.conversations.mock.getMockedMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.messagecomposer.KeyboardHeight
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.util.permission.CallingAudioRequestFlow
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import okio.Path
import okio.Path.Companion.toPath

@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel,
    commonTopAppBarViewModel: CommonTopAppBarViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val showDialog = remember { mutableStateOf(ConversationScreenDialogType.NONE) }

    val startCallAudioPermissionCheck = StartCallAudioBluetoothPermissionCheckFlow {
        conversationViewModel.navigateToInitiatingCallScreen()
    }
    val uiState = conversationViewModel.conversationViewState

    LaunchedEffect(conversationViewModel.savedStateHandle) {
        conversationViewModel.checkPendingActions()
    }

    when (showDialog.value) {
        ConversationScreenDialogType.ONGOING_ACTIVE_CALL -> {
            OngoingActiveCallDialog(onJoinAnyways = {
                conversationViewModel.navigateToInitiatingCallScreen()
                showDialog.value = ConversationScreenDialogType.NONE
            }, onDialogDismiss = {
                showDialog.value = ConversationScreenDialogType.NONE
            })
        }
        ConversationScreenDialogType.NO_CONNECTIVITY -> {
            CoreFailureErrorDialog(coreFailure = NetworkFailure.NoNetworkConnection(null)) {
                showDialog.value = ConversationScreenDialogType.NONE
            }
        }
        ConversationScreenDialogType.NONE -> {}
    }

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = conversationViewModel::onMessageChanged,
        onSendButtonClicked = conversationViewModel::sendMessage,
        onSendAttachment = conversationViewModel::sendAttachmentMessage,
        onDownloadAsset = conversationViewModel::downloadOrFetchAssetToInternalStorage,
        onImageFullScreenMode = conversationViewModel::navigateToGallery,
        onBackButtonClick = conversationViewModel::navigateBack,
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog,
        onStartCall = { startCallIfPossible(conversationViewModel, showDialog, startCallAudioPermissionCheck, coroutineScope) },
        onJoinCall = conversationViewModel::joinOngoingCall,
        onSnackbarMessage = conversationViewModel::onSnackbarMessage,
        onSnackbarMessageShown = conversationViewModel::clearSnackbarMessage,
        onDropDownClick = conversationViewModel::navigateToDetails,
        tempCachePath = conversationViewModel.provideTempCachePath(),
        onOpenProfile = conversationViewModel::navigateToProfile,
        onUpdateConversationReadDate = conversationViewModel::updateConversationReadDate,
        isConversationMember = conversationViewModel.isConversationMemberState,
        commonTopAppBarViewModel = commonTopAppBarViewModel
    )

    DeleteMessageDialog(
        state = conversationViewModel.deleteMessageDialogsState,
        actions = conversationViewModel.deleteMessageHelper
    )
    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationViewModel::onSaveFile,
        onOpenFileWithExternalApp = conversationViewModel::onOpenFileWithExternalApp,
        hideOnAssetDownloadedDialog = conversationViewModel::hideOnAssetDownloadedDialog
    )
}

private fun startCallIfPossible(
    conversationViewModel: ConversationViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    startCallAudioPermissionCheck: CallingAudioRequestFlow,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        if (!conversationViewModel.hasStableConnectivity()) {
            showDialog.value = ConversationScreenDialogType.NO_CONNECTIVITY
        } else {
            conversationViewModel.establishedCallConversationId?.let {
                showDialog.value = ConversationScreenDialogType.ONGOING_ACTIVE_CALL
            } ?: run {
                startCallAudioPermissionCheck.launch()
            }
        }
    }
}

@Composable
private fun StartCallAudioBluetoothPermissionCheckFlow(
    onStartCall: () -> Unit
) = rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
    onStartCall()
}) {
    //TODO display an error dialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Suppress("LongParameterList")
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onBackButtonClick: () -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onStartCall: () -> Unit,
    onJoinCall: () -> Unit,
    onSnackbarMessage: (ConversationSnackbarMessages) -> Unit,
    onSnackbarMessageShown: () -> Unit,
    onDropDownClick: () -> Unit,
    tempCachePath: Path,
    onOpenProfile: (MessageSource, UserId) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    isConversationMember: Boolean,
    commonTopAppBarViewModel: CommonTopAppBarBaseViewModel
) {
    val conversationScreenState = rememberConversationScreenState()

    with(conversationViewState) {
        val connectionStateOrNull = (conversationDetailsData as? ConversationDetailsData.OneOne)?.connectionState

        MenuModalSheetLayout(
            sheetState = conversationScreenState.modalBottomSheetState,
            coroutineScope = conversationScreenState.coroutineScope,
            menuItems = EditMessageMenuItems(
                isMyMessage = conversationScreenState.isSelectedMessageMyMessage(),
                onCopyMessage = conversationScreenState::copyMessage,
                onDeleteMessage = {
                    conversationScreenState.hideEditContextMenu()
                    onDeleteMessage(
                        conversationScreenState.selectedMessage?.messageHeader!!.messageId,
                        conversationScreenState.isSelectedMessageMyMessage()
                    )
                }
            ),
            content = {
                BoxWithConstraints {
                    val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }
                    val fullScreenHeight: Dp = remember { currentScreenHeight }

                    // when ConversationScreen is composed for the first time we do not know the height
                    // until users opens the keyboard
                    var keyboardHeight: KeyboardHeight by remember {
                        mutableStateOf(KeyboardHeight.NotKnown)
                    }

                    // if the currentScreenHeight is smaller than the initial fullScreenHeight
                    // and we don't know the keyboard height yet
                    // calculated at the first composition of the ConversationScreen, then we know the keyboard size
                    if (keyboardHeight is KeyboardHeight.NotKnown && currentScreenHeight < fullScreenHeight) {
                        val difference = fullScreenHeight - currentScreenHeight
                        if (difference > KeyboardHeight.DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET)
                            keyboardHeight = KeyboardHeight.Known(difference)
                    }

                    Scaffold(
                        topBar = {
                            Column {
                                CommonTopAppBar(commonTopAppBarViewModel = commonTopAppBarViewModel as CommonTopAppBarViewModel)
                                ConversationScreenTopAppBar(
                                    title = conversationName.asString(),
                                    avatar = {
                                        when (conversationAvatar) {
                                            is ConversationAvatar.Group ->
                                                GroupConversationAvatar(
                                                    color = colorsScheme().conversationColor(id = conversationAvatar.conversationId)
                                                )
                                            is ConversationAvatar.OneOne -> UserProfileAvatar(
                                                UserAvatarData(
                                                    asset = conversationAvatar.avatarAsset,
                                                    availabilityStatus = conversationAvatar.status,
                                                    connectionState = connectionStateOrNull
                                                )
                                            )
                                            ConversationAvatar.None -> Box(modifier = Modifier.size(dimensions().userAvatarDefaultSize))
                                        }
                                    },
                                    onBackButtonClick = onBackButtonClick,
                                    onDropDownClick = onDropDownClick,
                                    isDropDownEnabled = conversationViewState.conversationDetailsData !is ConversationDetailsData.None,
                                    onSearchButtonClick = { },
                                    onPhoneButtonClick = onStartCall,
                                    hasOngoingCall = hasOngoingCall,
                                    onJoinCallButtonClick = onJoinCall,
                                    isUserBlocked = connectionStateOrNull == ConnectionState.BLOCKED
                                )
                            }
                        },
                        snackbarHost = {
                            SwipeDismissSnackbarHost(
                                hostState = conversationScreenState.snackBarHostState,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        content = { internalPadding ->
                            Box(modifier = Modifier.padding(internalPadding)) {
                                ConversationScreenContent(
                                    keyboardHeight = keyboardHeight,
                                    messages = messages,
                                    lastUnreadMessage = lastUnreadMessage,
                                    onMessageChanged = onMessageChanged,
                                    messageText = conversationViewState.messageText,
                                    onSendButtonClicked = onSendButtonClicked,
                                    onShowContextMenu = conversationScreenState::showEditContextMenu,
                                    onSendAttachment = onSendAttachment,
                                    onDownloadAsset = onDownloadAsset,
                                    onImageFullScreenMode = onImageFullScreenMode,
                                    conversationState = conversationViewState,
                                    onMessageComposerError = onSnackbarMessage,
                                    onSnackbarMessageShown = onSnackbarMessageShown,
                                    conversationScreenState = conversationScreenState,
                                    isFileSharingEnabled = isFileSharingEnabled,
                                    tempCachePath = tempCachePath,
                                    isUserBlocked = connectionStateOrNull == ConnectionState.BLOCKED,
                                    isConversationMember = isConversationMember,
                                    onOpenProfile = onOpenProfile,
                                    onUpdateConversationReadDate = onUpdateConversationReadDate
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ConversationScreenContent(
    keyboardHeight: KeyboardHeight,
    messages: List<UIMessage>,
    lastUnreadMessage: UIMessage?,
    onMessageChanged: (String) -> Unit,
    messageText: String,
    onSendButtonClicked: () -> Unit,
    onShowContextMenu: (UIMessage) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (MessageSource, UserId) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    conversationState: ConversationViewState,
    onSnackbarMessageShown: () -> Unit,
    conversationScreenState: ConversationScreenState,
    isFileSharingEnabled: Boolean,
    isUserBlocked: Boolean,
    isConversationMember: Boolean,
    tempCachePath: Path,
    onUpdateConversationReadDate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    conversationState.onSnackbarMessage?.let { messageCode ->
        val (message, actionLabel) = getSnackbarMessage(messageCode)
        LaunchedEffect(conversationState.onSnackbarMessage) {
            val snackbarResult = conversationScreenState.snackBarHostState.showSnackbar(message = message, actionLabel = actionLabel)
            when {
                // Show downloads folder when clicking on Snackbar cta button
                messageCode is OnFileDownloaded && snackbarResult == SnackbarResult.ActionPerformed -> {
                    context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    onSnackbarMessageShown()
                }
                snackbarResult == SnackbarResult.Dismissed -> onSnackbarMessageShown()
            }
        }
    }

    val lazyListState = rememberSaveable(lastUnreadMessage, saver = LazyListState.Saver) {
        LazyListState(
            if (lastUnreadMessage != null) messages.indexOf(lastUnreadMessage) else 0,
            0
        )
    }

    LaunchedEffect(messages) {
        lazyListState.animateScrollToItem(0)
    }

    MessageComposer(
        keyboardHeight = keyboardHeight,
        content = {
            MessageList(
                messages = messages,
                lastUnreadMessage,
                lazyListState = lazyListState,
                onShowContextMenu = onShowContextMenu,
                onDownloadAsset = onDownloadAsset,
                onImageFullScreenMode = onImageFullScreenMode,
                onOpenProfile = onOpenProfile,
                onUpdateConversationReadDate = onUpdateConversationReadDate
            )
        },
        messageText = messageText,
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked,
        onSendAttachment = onSendAttachment,
        onMessageComposerError = onMessageComposerError,
        onMessageComposerInputStateChange = { messageComposerState ->
            if (messageComposerState.to == MessageComposeInputState.Active &&
                messageComposerState.from == MessageComposeInputState.Enabled
            ) {
                coroutineScope.launch { lazyListState.animateScrollToItem(messages.size) }
            }
        },
        isFileSharingEnabled = isFileSharingEnabled,
        tempCachePath = tempCachePath,
        isUserBlocked = isUserBlocked,
        isConversationMember = isConversationMember,
        securityClassificationType = conversationState.securityClassificationType
    )
}

@Composable
private fun getSnackbarMessage(messageCode: ConversationSnackbarMessages): Pair<String, String?> {
    val msg = when (messageCode) {
        is OnFileDownloaded -> stringResource(R.string.conversation_on_file_downloaded, messageCode.assetName ?: "")
        is ErrorMaxAssetSize -> stringResource(R.string.error_conversation_max_asset_size_limit, messageCode.maxLimitInMB)
        ErrorMaxImageSize -> stringResource(R.string.error_conversation_max_image_size_limit)
        ErrorSendingImage -> stringResource(R.string.error_conversation_sending_image)
        ErrorSendingAsset -> stringResource(R.string.error_conversation_sending_asset)
        ErrorDownloadingAsset -> stringResource(R.string.error_conversation_downloading_asset)
        ErrorOpeningAssetFile -> stringResource(R.string.error_conversation_opening_asset_file)
        ErrorDeletingMessage -> stringResource(R.string.error_conversation_deleting_message)
        ErrorPickingAttachment -> stringResource(R.string.error_conversation_generic)
    }
    val actionLabel = when (messageCode) {
        is OnFileDownloaded -> stringResource(R.string.label_show)
        else -> null
    }
    return msg to actionLabel
}

@Composable
fun MessageList(
    messages: List<UIMessage>,
    lastUnreadMessage: UIMessage?,
    lazyListState: LazyListState,
    onShowContextMenu: (UIMessage) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (MessageSource, UserId) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit
) {
    if (messages.isNotEmpty() && lastUnreadMessage != null) {
        LaunchedEffect(lazyListState.isScrollInProgress) {
            if (!lazyListState.isScrollInProgress) {
                val lastVisibleMessage = messages[lazyListState.firstVisibleItemIndex]

                val lastVisibleMessageInstant = Instant.parse(lastVisibleMessage.messageHeader.messageTime.utcISO)
                val lastUnreadMessageInstant = Instant.parse(lastUnreadMessage.messageHeader.messageTime.utcISO)

                if (lastVisibleMessageInstant >= lastUnreadMessageInstant) {
                    onUpdateConversationReadDate(lastVisibleMessage.messageHeader.messageTime.utcISO)
                }
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        reverseLayout = true,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        items(messages, key = {
            it.messageHeader.messageId
        }) { message ->
            if (message.messageContent is MessageContent.SystemMessage) {
                SystemMessageItem(message = message.messageContent)
            } else {
                MessageItem(
                    message = message,
                    onLongClicked = onShowContextMenu,
                    onAssetMessageClicked = onDownloadAsset,
                    onImageMessageClicked = onImageFullScreenMode,
                    onAvatarClicked = onOpenProfile
                )
            }
        }
    }
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        conversationViewState = ConversationViewState(
            conversationName = UIText.DynamicString("Some test conversation"),
            messages = getMockedMessages(),
        ),
        onMessageChanged = {},
        onSendButtonClicked = {},
        onSendAttachment = {},
        onDownloadAsset = {},
        onImageFullScreenMode = { _, _ -> },
        onBackButtonClick = {},
        onDeleteMessage = { _, _ -> },
        onStartCall = {},
        onJoinCall = {},
        onSnackbarMessage = {},
        onSnackbarMessageShown = {},
        onDropDownClick = {},
        tempCachePath = "".toPath(),
        onOpenProfile = { _, _ -> },
        onUpdateConversationReadDate = {},
        isConversationMember = true,
        commonTopAppBarViewModel = object : CommonTopAppBarBaseViewModel() {}
    )
}
