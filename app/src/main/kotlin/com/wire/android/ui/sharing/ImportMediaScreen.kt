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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.sharing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.authentication.welcome.WelcomeScreenNavArgs
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.ErrorIcon
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.remove.RemoveIcon
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.AssetTooLargeDialog
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel
import com.wire.android.ui.home.conversations.media.preview.AssetTilePreview
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversations.selfdeletion.selfDeletionMenuItems
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.common.previewConversationFolders
import com.wire.android.ui.home.conversationslist.common.previewConversationFoldersFlow
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.home.newconversation.common.SendContentButton
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.extension.getActivity
import com.wire.android.util.ui.LinkText
import com.wire.android.util.ui.LinkTextData
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.util.isPositiveNotNull
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okio.Path.Companion.toPath

@RootNavGraph
@WireDestination
@Composable
fun ImportMediaScreen(
    navigator: Navigator,
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel = hiltViewModel(),
) {
    when (val fileSharingRestrictedState = featureFlagNotificationViewModel.featureFlagState.isFileSharingState) {
        FeatureFlagState.FileSharingState.Loading -> {
            ImportMediaLoadingContent(
                navigateBack = navigator.finish
            )
        }

        FeatureFlagState.FileSharingState.NoUser -> {
            ImportMediaLoggedOutContent(
                fileSharingRestrictedState = fileSharingRestrictedState,
                navigateBack = navigator.finish,
                openWireAction = {
                    navigator.navigate(NavigationCommand(WelcomeScreenDestination(WelcomeScreenNavArgs()), BackStackMode.CLEAR_WHOLE))
                }
            )
        }

        FeatureFlagState.FileSharingState.DisabledByTeam,
        FeatureFlagState.FileSharingState.AllowAll,
        is FeatureFlagState.FileSharingState.AllowSome -> {
            ImportMediaAuthenticatedContent(
                navigator = navigator,
                isRestrictedInTeam = fileSharingRestrictedState == FeatureFlagState.FileSharingState.DisabledByTeam,
            )
        }
    }

    BackHandler { navigator.finish() }
}

@Composable
private fun ImportMediaLoadingContent(navigateBack: () -> Unit) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                onNavigationPressed = navigateBack,
                navigationIconType = NavigationIconType.Close(),
                title = stringResource(id = R.string.import_media_content_title),
            )
        },
        modifier = Modifier.background(colorsScheme().background),
        content = { internalPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(internalPadding)
                    .padding(horizontal = dimensions().spacing48x)
            ) {
                WireCircularProgressIndicator(
                    progressColor = MaterialTheme.wireColorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    size = dimensions().spacing32x
                )
            }
        }
    )
}

@Composable
private fun ImportMediaAuthenticatedContent(
    navigator: Navigator,
    isRestrictedInTeam: Boolean,
    checkAssetRestrictionsViewModel: CheckAssetRestrictionsViewModel = hiltViewModel(),
    importMediaViewModel: ImportMediaAuthenticatedViewModel = hiltViewModel(),
) {
    if (isRestrictedInTeam) {
        ImportMediaRestrictedContent(
            importMediaAuthenticatedState = importMediaViewModel.importMediaState,
            avatarAsset = null,
            navigateBack = navigator.finish
        )
    } else {
        ImportMediaRegularContent(
            importMediaAuthenticatedState = importMediaViewModel.importMediaState,
            searchQueryTextState = importMediaViewModel.searchQueryTextState,
            avatarAsset = importMediaViewModel.avatarAsset,
            onConversationClicked = importMediaViewModel::onConversationClicked,
            checkRestrictionsAndSendImportedMedia = {
                with(importMediaViewModel.importMediaState) {
                    selectedConversationItem.firstOrNull()?.let { conversationItem ->
                        checkAssetRestrictionsViewModel.checkRestrictions(
                            importedMediaList = importedAssets,
                            onSuccess = {
                                navigator.navigate(
                                    NavigationCommand(
                                        ConversationScreenDestination(
                                            ConversationNavArgs(
                                                conversationId = conversationItem,
                                                pendingBundles = ArrayList(it),
                                                pendingTextBundle = importedText
                                            )
                                        ),
                                        BackStackMode.REMOVE_CURRENT_AND_REPLACE
                                    ),
                                )
                            }
                        )
                    }
                }
            },
            onNewSelfDeletionTimerPicked = importMediaViewModel::onNewSelfDeletionTimerPicked,
            infoMessage = importMediaViewModel.infoMessage,
            navigateBack = navigator.finish,
            onRemoveAsset = importMediaViewModel::onRemove
        )
        AssetTooLargeDialog(
            dialogState = checkAssetRestrictionsViewModel.assetTooLargeDialogState,
            hideDialog = checkAssetRestrictionsViewModel::hideDialog
        )

        val context = LocalContext.current
        with(importMediaViewModel.importMediaState) {
            LaunchedEffect(isImportingData()) {
                if (importedAssets.isEmpty() || importedText.isNullOrEmpty()) {
                    context.getActivity()
                        ?.let { activity -> importMediaViewModel.handleReceivedDataFromSharingIntent(activity) }
                }
            }
        }
    }
}

@Composable
fun ImportMediaRestrictedContent(
    importMediaAuthenticatedState: ImportMediaAuthenticatedState,
    avatarAsset: ImageAsset.UserAvatarAsset?,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    with(importMediaAuthenticatedState) {
        WireScaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = dimensions().spacing0x,
                    onNavigationPressed = navigateBack,
                    navigationIconType = NavigationIconType.Close(),
                    title = stringResource(id = R.string.import_media_content_title),
                    actions = {
                        UserProfileAvatar(
                            avatarData = UserAvatarData(avatarAsset),
                            clickable = remember { Clickable(enabled = false) { } }
                        )
                    }
                )
            },
            modifier = modifier.background(colorsScheme().background),
            content = { internalPadding ->
                FileSharingRestrictedContent(
                    internalPadding,
                    FeatureFlagState.FileSharingState.DisabledByTeam,
                    navigateBack
                )
            }
        )
    }
}

@Composable
fun ImportMediaRegularContent(
    importMediaAuthenticatedState: ImportMediaAuthenticatedState,
    avatarAsset: ImageAsset.UserAvatarAsset?,
    searchQueryTextState: TextFieldState,
    onConversationClicked: (conversationItem: ConversationItem) -> Unit,
    checkRestrictionsAndSendImportedMedia: () -> Unit,
    onNewSelfDeletionTimerPicked: (selfDeletionDuration: SelfDeletionDuration) -> Unit,
    infoMessage: SharedFlow<SnackBarMessage>,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveAsset: (index: Int) -> Unit
) {

    val importMediaScreenState = rememberImportMediaScreenState()
    val lazyListState = rememberLazyListState()
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation

    with(importMediaAuthenticatedState) {
        WireScaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = lazyListState.topBarElevation(maxAppBarElevation),
                    onNavigationPressed = navigateBack,
                    navigationIconType = NavigationIconType.Close(),
                    title = stringResource(id = R.string.import_media_content_title),
                    actions = {
                        UserProfileAvatar(
                            avatarData = UserAvatarData(avatarAsset),
                            clickable = remember { Clickable(enabled = false) { } }
                        )
                    },
                    bottomContent = {
                        ImportMediaTopBarContent(
                            state = importMediaAuthenticatedState,
                            searchBarState = importMediaScreenState.searchBarState,
                            searchQueryTextState = searchQueryTextState,
                            onRemoveAsset = onRemoveAsset,
                        )
                    }
                )
            },
            modifier = modifier.background(colorsScheme().background),
            content = { internalPadding ->
                ImportMediaContent(
                    state = this,
                    internalPadding = internalPadding,
                    onConversationClicked = onConversationClicked,
                    lazyListState = lazyListState,
                )
            },
            bottomBar = {
                ImportMediaBottomBar(
                    state = this,
                    importMediaScreenState = importMediaScreenState,
                    checkRestrictionsAndSendImportedMedia = checkRestrictionsAndSendImportedMedia
                )
            }
        )
        BackHandler(enabled = importMediaScreenState.searchBarState.isSearchActive) {
            importMediaScreenState.searchBarState.closeSearch()
        }
        WireModalSheetLayout(
            sheetState = importMediaScreenState.bottomSheetState,
            sheetContent = {
                WireMenuModalSheetContent(
                    menuItems = selfDeletionMenuItems(
                        currentlySelected = importMediaAuthenticatedState.selfDeletingTimer.duration.toSelfDeletionDuration(),
                        onSelfDeletionDurationChanged = remember {
                            {
                                importMediaScreenState.bottomSheetState.hide {
                                    onNewSelfDeletionTimerPicked(it)
                                }
                            }
                        },
                    )
                )
            },
        )
    }
    SnackBarMessage(infoMessage, importMediaScreenState.snackbarHostState)
}

@Composable
fun ImportMediaLoggedOutContent(
    fileSharingRestrictedState: FeatureFlagState.FileSharingState,
    navigateBack: () -> Unit,
    openWireAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                onNavigationPressed = navigateBack,
                navigationIconType = NavigationIconType.Close(),
                title = stringResource(id = R.string.import_media_content_title),
            )
        },
        modifier = modifier.background(colorsScheme().background),
        content = { internalPadding ->
            FileSharingRestrictedContent(
                internalPadding = internalPadding,
                sharingRestrictedState = fileSharingRestrictedState,
                openWireAction = openWireAction
            )
        }
    )
}

@Composable
fun FileSharingRestrictedContent(
    internalPadding: PaddingValues,
    sharingRestrictedState: FeatureFlagState.FileSharingState,
    openWireAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_file_sharing_restricted_learn_more)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(internalPadding)
            .padding(horizontal = dimensions().spacing48x)
    ) {
        val textRes =
            if (sharingRestrictedState == FeatureFlagState.FileSharingState.NoUser) {
                R.string.file_sharing_restricted_description_no_users
            } else {
                R.string.file_sharing_restricted_description_by_team
            }
        Text(
            text = stringResource(textRes),
            textAlign = TextAlign.Center,
            style = MaterialTheme.wireTypography.body01,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(dimensions().spacing16x))

        if (sharingRestrictedState == FeatureFlagState.FileSharingState.NoUser) {
            WirePrimaryButton(
                onClick = openWireAction,
                text = stringResource(R.string.file_sharing_restricted_button_text_no_users),
                fillMaxWidth = false
            )
        } else {
            LinkText(
                linkTextData = listOf(
                    LinkTextData(text = stringResource(R.string.label_learn_more),
                        tag = "learn_more",
                        annotation = learnMoreUrl,
                        onClick = { CustomTabsHelper.launchUrl(context, learnMoreUrl) }
                    )
                ),
                textColor = colorsScheme().onBackground
            )
        }
    }
}

@Composable
private fun ImportMediaBottomBar(
    state: ImportMediaAuthenticatedState,
    importMediaScreenState: ImportMediaScreenState,
    checkRestrictionsAndSendImportedMedia: () -> Unit,
) {
    val selfDeletionTimer = state.selfDeletingTimer
    val shortDurationLabel = selfDeletionTimer.duration.toSelfDeletionDuration().shortLabel
    val mainButtonText = if (selfDeletionTimer.duration.isPositiveNotNull()) {
        "${stringResource(id = R.string.self_deleting_message_label)} (${shortDurationLabel.asString()})"
    } else {
        stringResource(id = R.string.import_media_send_button_title)
    }
    val buttonCount =
        if (state.importedAssets.isNotEmpty() || state.importedText != null) state.selectedConversationItem.size else 0
    SendContentButton(
        mainButtonText = mainButtonText,
        count = buttonCount,
        onMainButtonClick = checkRestrictionsAndSendImportedMedia,
        selfDeletionTimer = selfDeletionTimer,
        onSelfDeletionTimerClicked = importMediaScreenState.bottomSheetState::show,
    )
}

@Composable
fun ImportMediaTopBarContent(
    state: ImportMediaAuthenticatedState,
    searchBarState: SearchBarState,
    searchQueryTextState: TextFieldState,
    onRemoveAsset: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMultipleImport = state.importedAssets.size != 1

    Column(modifier = modifier) {
        if (state.isImporting) {
            Box(
                Modifier
                    .height(dimensions().spacing120x)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                WireCircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    progressColor = colorsScheme().primary,
                    size = dimensions().spacing24x
                )
            }
        } else if (!isMultipleImport) {
            Box(
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing16x)
                    .height(dimensions().spacing120x)
            ) {
                AssetTilePreview(
                    modifier = Modifier.fillMaxHeight(),
                    assetBundle = state.importedAssets.first().assetBundle,
                    showOnlyExtension = false,
                    onClick = {}
                )
            }
        } else {
            when (state.importedText.isNullOrBlank()) {
                true -> ImportAssetsCarrousel(state.importedAssets, onRemoveAsset)
                false -> ImportText(state.importedText)
            }
        }
        HorizontalDivider(
            color = colorsScheme().outline,
            thickness = 1.dp,
            modifier = Modifier.padding(top = dimensions().spacing12x)
        )
        SearchTopBar(
            isSearchActive = searchBarState.isSearchActive,
            searchBarHint = stringResource(
                R.string.search_bar_conversations_hint,
                stringResource(id = R.string.conversations_screen_title).lowercase()
            ),
            searchQueryTextState = searchQueryTextState,
            onActiveChanged = searchBarState::searchActiveChanged,
        )
    }
}

@Composable
private fun ImportMediaContent(
    state: ImportMediaAuthenticatedState,
    internalPadding: PaddingValues,
    onConversationClicked: (conversationItem: ConversationItem) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier
            .padding(internalPadding)
            .fillMaxSize()
    ) {
        val lazyPagingConversations = state.conversations.collectAsLazyPagingItems()
        ConversationList(
            modifier = Modifier.weight(1f),
            lazyListState = lazyListState,
            lazyPagingConversations = lazyPagingConversations,
            selectedConversations = state.selectedConversationItem,
            isSelectableList = true,
            onConversationSelectedOnRadioGroup = onConversationClicked,
            onOpenConversation = onConversationClicked,
            onEditConversation = {},
            onOpenUserProfile = {},
            onJoinCall = {},
            onAudioPermissionPermanentlyDenied = {}
        )
    }
}

@Composable
private fun ImportText(importedText: String) {
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .wrapContentSize()
            .height(dimensions().spacing120x)
            .verticalScroll(scrollState)
            .padding(vertical = dimensions().spacing8x, horizontal = dimensions().spacing16x),
    ) {
        Text(
            text = importedText,
            textAlign = TextAlign.Start,
            style = MaterialTheme.wireTypography.body01,
        )
    }
}

@Composable
private fun ImportAssetsCarrousel(
    importedItemsList: PersistentList<ImportedMediaAsset>,
    onRemoveAsset: (index: Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing120x),
        contentPadding = PaddingValues(start = dimensions().spacing8x, end = dimensions().spacing8x)
    ) {
        items(
            count = importedItemsList.size,
        ) { index ->
            Box(
                modifier = Modifier
                    .width(dimensions().spacing120x)
                    .fillMaxHeight()
            ) {
                val assetSize = dimensions().spacing120x - dimensions().spacing16x
                AssetTilePreview(
                    modifier = Modifier
                        .width(assetSize)
                        .height(assetSize)
                        .align(Alignment.Center),
                    assetBundle = importedItemsList[index].assetBundle,
                    showOnlyExtension = false,
                    onClick = {}
                )

                if (importedItemsList.size > 1) {
                    RemoveIcon(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onClick = { onRemoveAsset(index) },
                        contentDescription = stringResource(id = R.string.remove_asset_description)
                    )
                }
                if (importedItemsList[index].assetSizeExceeded != null) {
                    ErrorIcon(
                        stringResource(id = R.string.asset_attention_description),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun SnackBarMessage(
    infoMessages: SharedFlow<SnackBarMessage>,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        infoMessages.collect { message ->
            snackbarHostState.showSnackbar(message.uiText.asString(context.resources))
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImportMediaScreenLoggedOut() {
    WireTheme {
        ImportMediaLoggedOutContent(
            fileSharingRestrictedState = FeatureFlagState.FileSharingState.NoUser,
            navigateBack = {},
            openWireAction = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImportMediaScreenRestricted() {
    WireTheme {
        ImportMediaRestrictedContent(
            importMediaAuthenticatedState = ImportMediaAuthenticatedState(),
            avatarAsset = null,
            navigateBack = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImportMediaScreenRegular() {
    WireTheme {
        ImportMediaRegularContent(
            importMediaAuthenticatedState = ImportMediaAuthenticatedState(
                conversations = previewConversationFoldersFlow(),
                importedAssets = persistentListOf(
                    ImportedMediaAsset(
                        AssetBundle(
                            "key",
                            "image/png",
                            "".toPath(),
                            20,
                            "preview.png",
                            assetType = AttachmentType.IMAGE
                        ),
                        assetSizeExceeded = null
                    ),
                    ImportedMediaAsset(
                        AssetBundle(
                            "key1",
                            "video/mp4",
                            "".toPath(),
                            20,
                            "preview.mp4",
                            assetType = AttachmentType.VIDEO
                        ),
                        assetSizeExceeded = null
                    ),
                    ImportedMediaAsset(
                        AssetBundle(
                            "key2",
                            "audio/mp3",
                            "".toPath(),
                            24000000,
                            "preview.mp3",
                            assetType = AttachmentType.AUDIO
                        ),
                        assetSizeExceeded = 20
                    ),
                    ImportedMediaAsset(
                        AssetBundle(
                            "key3",
                            "document/pdf",
                            "".toPath(),
                            20,
                            "preview.pdf",
                            assetType = AttachmentType.GENERIC_FILE
                        ),
                        assetSizeExceeded = null
                    )
                ),
            ),
            avatarAsset = null,
            searchQueryTextState = rememberTextFieldState(),
            onConversationClicked = {},
            checkRestrictionsAndSendImportedMedia = {},
            onNewSelfDeletionTimerPicked = {},
            infoMessage = MutableSharedFlow(),
            onRemoveAsset = { _ -> },
            navigateBack = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImportMediaTextScreenRegular() {
    WireTheme {
        ImportMediaRegularContent(
            importMediaAuthenticatedState = ImportMediaAuthenticatedState(
                conversations = previewConversationFoldersFlow(list = previewConversationFolders(withFolders = false)),
                importedAssets = persistentListOf(),
                importedText = "This is a shared text message \n" +
                        "This is a second line with a veeeeeeeeeeeeeeeeeeeeeeeeeeery long shared text message"
            ),
            searchQueryTextState = rememberTextFieldState(),
            avatarAsset = null,
            onConversationClicked = {},
            checkRestrictionsAndSendImportedMedia = {},
            onNewSelfDeletionTimerPicked = {},
            infoMessage = MutableSharedFlow(),
            onRemoveAsset = { _ -> },
            navigateBack = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImportMediaBottomBar() {
    WireTheme {
        ImportMediaBottomBar(
            state = ImportMediaAuthenticatedState(),
            importMediaScreenState = rememberImportMediaScreenState(),
            checkRestrictionsAndSendImportedMedia = {},
        )
    }
}
