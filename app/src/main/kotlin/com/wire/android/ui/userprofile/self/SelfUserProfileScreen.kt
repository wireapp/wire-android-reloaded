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

package com.wire.android.ui.userprofile.self

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.welcome.WelcomeScreenNavArgs
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.avatar.UserStatusIndicator
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dialogs.ProgressDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.AppSettingsScreenDestination
import com.wire.android.ui.destinations.AvatarPickerScreenDestination
import com.wire.android.ui.destinations.MyAccountScreenDestination
import com.wire.android.ui.destinations.SelfQRCodeScreenDestination
import com.wire.android.ui.destinations.TeamMigrationScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.legalhold.banner.LegalHoldPendingBanner
import com.wire.android.ui.legalhold.banner.LegalHoldSubjectBanner
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedDialog
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedState
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectProfileSelfDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel.ErrorCodes.DownloadUserInfoError
import com.wire.android.ui.userprofile.self.dialog.ChangeStatusDialogContent
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.ui.userprofile.self.model.OtherAccount
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

@RootNavGraph
@WireDestination(
    style = PopUpNavigationAnimation::class,
)
@Composable
@SuppressLint("ComposeModifierMissing")
fun SelfUserProfileScreen(
    navigator: Navigator,
    avatarPickerResultRecipient: ResultRecipient<AvatarPickerScreenDestination, String?>,
    viewModelSelf: SelfUserProfileViewModel = hiltViewModel(),
    legalHoldRequestedViewModel: LegalHoldRequestedViewModel = hiltViewModel()
) {
    val legalHoldSubjectDialogState = rememberVisibilityState<Unit>()

    LaunchedEffect(Unit) {
        // Check if the user is able to migrate to a team account, every time the screen is shown
        viewModelSelf.checkIfUserAbleToMigrateToTeamAccount()
    }

    SelfUserProfileContent(
        state = viewModelSelf.userProfileState,
        onCloseClick = navigator::navigateBack,
        logout = { viewModelSelf.logout(it, NavigationSwitchAccountActions(navigator::navigate)) },
        onChangeUserProfilePicture = {
            navigator.navigate(
                NavigationCommand(
                    AvatarPickerScreenDestination
                )
            )
        },
        onEditClick = { navigator.navigate(NavigationCommand(AppSettingsScreenDestination)) },
        onStatusClicked = viewModelSelf::changeStatusClick,
        onAddAccountClick = { navigator.navigate(NavigationCommand(WelcomeScreenDestination(WelcomeScreenNavArgs()))) },
        dismissStatusDialog = viewModelSelf::dismissStatusDialog,
        onStatusChange = viewModelSelf::changeStatus,
        onNotShowRationaleAgainChange = viewModelSelf::dialogCheckBoxStateChanged,
        onMessageShown = viewModelSelf::clearErrorMessage,
        onLegalHoldAcceptClick = legalHoldRequestedViewModel::show,
        onLegalHoldLearnMoreClick = remember { { legalHoldSubjectDialogState.show(Unit) } },
        onOtherAccountClick = {
            viewModelSelf.switchAccount(
                it,
                NavigationSwitchAccountActions(navigator::navigate)
            )
        },
        onQrCodeClick = {
            viewModelSelf.trackQrCodeClick()
            navigator.navigate(NavigationCommand(SelfQRCodeScreenDestination(viewModelSelf.userProfileState.userName)))
        },
        onCreateAccount = {
            viewModelSelf.sendPersonalToTeamMigrationEvent()
            navigator.navigate(NavigationCommand(TeamMigrationScreenDestination))
        },
        onAccountDetailsClick = { navigator.navigate(NavigationCommand(MyAccountScreenDestination)) },
        isUserInCall = viewModelSelf::isUserInCall,
    )

    avatarPickerResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from avatar picker in SelfUserProfileScreen")
            }

            is NavResult.Value -> {
                result.value?.let { avatarAssetId ->
                    viewModelSelf.reloadNewPickedAvatar(
                        avatarAssetId = avatarAssetId
                    )
                }
            }
        }
    }

    if (legalHoldRequestedViewModel.state is LegalHoldRequestedState.Visible) {
        LegalHoldRequestedDialog(
            state = legalHoldRequestedViewModel.state as LegalHoldRequestedState.Visible,
            passwordTextState = legalHoldRequestedViewModel.passwordTextState,
            notNowClicked = legalHoldRequestedViewModel::notNowClicked,
            acceptClicked = legalHoldRequestedViewModel::acceptClicked,
        )
    }
    VisibilityState(legalHoldSubjectDialogState) {
        LegalHoldSubjectProfileSelfDialog(legalHoldSubjectDialogState::dismiss)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelfUserProfileContent(
    state: SelfUserProfileState,
    onCloseClick: () -> Unit = {},
    logout: (Boolean) -> Unit = {},
    onChangeUserProfilePicture: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onStatusClicked: (UserAvailabilityStatus) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    dismissStatusDialog: () -> Unit = {},
    onStatusChange: (UserAvailabilityStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {},
    onMessageShown: () -> Unit = {},
    onLegalHoldAcceptClick: () -> Unit = {},
    onLegalHoldLearnMoreClick: () -> Unit = {},
    onOtherAccountClick: (UserId) -> Unit = {},
    onQrCodeClick: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onAccountDetailsClick: () -> Unit = {},
    isUserInCall: () -> Boolean
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val uriHandler = LocalUriHandler.current

    state.errorMessageCode?.let { errorCode ->
        val errorMessage = mapErrorCodeToString(errorCode)
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage)
            onMessageShown()
        }
    }
    val scrollState = rememberScrollState()
    val logoutOptionsDialogState = rememberVisibilityState<LogoutOptionsDialogState>()

    WireScaffold(
        topBar = {
            SelfUserProfileTopBar(
                onCloseClick = onCloseClick,
                onLogoutClick = remember {
                    {
                        logoutOptionsDialogState.show(
                            logoutOptionsDialogState.savedState ?: LogoutOptionsDialogState()
                        )
                    }
                }
            )
        }
    ) { internalPadding ->
        with(state) {
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollable(state = scrollState, orientation = Orientation.Vertical)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(internalPadding)
            ) {
                val selectLabel = stringResource(R.string.content_description_select_label)
                LazyColumn(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .scrollable(state = scrollState, orientation = Orientation.Vertical)
                ) {
                    if (state.isAbleToMigrateToTeamAccount) {
                        stickyHeader {
                            Column(
                                modifier = Modifier
                                    .padding(
                                        top = dimensions().spacing16x,
                                        start = dimensions().spacing16x,
                                        end = dimensions().spacing16x
                                    )
                            ) {
                                CreateTeamInfoCard(onCreateAccount)
                            }
                        }
                    }
                    stickyHeader {
                        UserProfileInfo(
                            userId = state.userId,
                            isLoading = state.isAvatarLoading,
                            avatarAsset = state.avatarAsset,
                            fullName = fullName,
                            userName = userName,
                            teamName = teamName,
                            onUserProfileClick = onChangeUserProfilePicture,
                            editableState = EditableState.IsEditable(onEditClick),
                            onQrCodeClick = onQrCodeClick,
                            accentId = accentId
                        )
                    }
                    if (state.legalHoldStatus != LegalHoldUIState.None) {
                        stickyHeader {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = dimensions().spacing8x)
                            ) {
                                when (state.legalHoldStatus) {
                                    LegalHoldUIState.Active -> LegalHoldSubjectBanner(onClick = onLegalHoldLearnMoreClick)
                                    LegalHoldUIState.Pending -> LegalHoldPendingBanner(onClick = onLegalHoldAcceptClick)
                                    LegalHoldUIState.None -> {
                                        /* no banner */
                                    }
                                }
                            }
                        }
                    }
                    if (!state.teamName.isNullOrBlank()) {
                        stickyHeader {
                            CurrentSelfUserStatus(
                                userStatus = status,
                                onStatusClicked = onStatusClicked,
                            )
                        }
                    }
                    stickyHeader {
                        VerticalSpace.x8()
                        Box(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x)) {
                            AccountDetailButton(onAccountDetailsClick = onAccountDetailsClick)
                        }
                    }
                    if (state.otherAccounts.isNotEmpty()) {
                        stickyHeader {
                            VerticalSpace.x16()
                            OtherAccountsHeader()
                        }
                        items(
                            items = otherAccounts,
                            itemContent = { account ->
                                OtherAccountItem(
                                    account = account,
                                    clickable = remember {
                                        Clickable(
                                            enabled = true,
                                            onClickDescription = selectLabel,
                                            onClick = {
                                                if (isUserInCall()) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.cant_switch_account_in_call),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    onOtherAccountClick(account.id)
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }

                Divider(color = MaterialTheme.wireColorScheme.outline)

                Column(
                    modifier = Modifier.padding(dimensions().spacing16x),
                    verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                ) {
                    if (teamUrl != null) {
                        ManageTeamButton { uriHandler.openUri(teamUrl) }
                    }
                    NewTeamButton(onAddAccountClick, isUserInCall, context)
                }
            }
            ChangeStatusDialogContent(
                data = statusDialogData,
                dismiss = dismissStatusDialog,
                onStatusChange = onStatusChange,
                onNotShowRationaleAgainChange = onNotShowRationaleAgainChange
            )

            LogoutOptionsDialog(
                dialogState = logoutOptionsDialogState,
                logout = logout
            )

            LoggingOutDialog(isLoggingOut)
        }
    }
}

@Composable
private fun mapErrorCodeToString(errorCode: ErrorCodes): String {
    return when (errorCode) {
        DownloadUserInfoError -> stringResource(R.string.error_downloading_self_user_profile_picture)
        // Add more future errors for a more granular error handling
        else -> stringResource(R.string.error_unknown_title)
    }
}

@Composable
private fun SelfUserProfileTopBar(
    onCloseClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = stringResource(id = R.string.user_profile_title),
        navigationIconType = NavigationIconType.Close(R.string.content_description_self_profile_close),
        titleContentDescription = stringResource(R.string.content_description_self_profile_heading),
        elevation = 0.dp,
        actions = {
            WireSecondaryButton(
                onClick = onLogoutClick,
                text = stringResource(R.string.user_profile_logout),
                fillMaxWidth = false,
                minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                state = WireButtonState.Error,
                clickBlockParams = ClickBlockParams(
                    blockWhenSyncing = false,
                    blockWhenConnecting = false
                ),
            )
        }
    )
}

@Composable
private fun CurrentSelfUserStatus(
    userStatus: UserAvailabilityStatus,
    onStatusClicked: (UserAvailabilityStatus) -> Unit,
) {
    val items = listOf(
        UserAvailabilityStatus.AVAILABLE,
        UserAvailabilityStatus.BUSY,
        UserAvailabilityStatus.AWAY,
        UserAvailabilityStatus.NONE
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        FolderHeader(stringResource(R.string.user_profile_status_availability))

        WireDropDown(
            items = items.map {
                when (it) {
                    UserAvailabilityStatus.AVAILABLE -> stringResource(R.string.user_profile_status_available)
                    UserAvailabilityStatus.BUSY -> stringResource(R.string.user_profile_status_busy)
                    UserAvailabilityStatus.AWAY -> stringResource(R.string.user_profile_status_away)
                    UserAvailabilityStatus.NONE -> stringResource(R.string.user_profile_status_none)
                }
            },
            defaultItemIndex = items.indexOf(userStatus),
            label = null,
            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
            autoUpdateSelection = false,
            showDefaultTextIndicator = false,
            leadingCompose = { index -> UserStatusIndicator(items[index]) },
            onChangeClickDescription = stringResource(R.string.content_description_self_profile_change_status)
        ) { selectedIndex ->
            onStatusClicked(items[selectedIndex])
        }
    }
}

@Composable
private fun ManageTeamButton(
    onManageTeamClick: () -> Unit
) {
    WireSecondaryButton(
        text = stringResource(R.string.user_profile_account_management),
        onClickDescription = stringResource(R.string.content_description_self_profile_manage_team_btn),
        onClick = onManageTeamClick
    )
}

@Composable
private fun NewTeamButton(
    onAddAccountClick: () -> Unit,
    isUserIdCall: () -> Boolean,
    context: Context
) {
    WirePrimaryButton(
        modifier = Modifier
            .testTag("New Team or Account"),
        text = stringResource(R.string.user_profile_new_account_text),
        onClickDescription = stringResource(R.string.content_description_self_profile_new_account_btn),
        onClick = remember {
            {
                if (isUserIdCall()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.cant_switch_account_in_call),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onAddAccountClick()
                }
            }
        }
    )
}

@Composable
private fun AccountDetailButton(
    onAccountDetailsClick: () -> Unit,
) {
    WireSecondaryButton(
        modifier = Modifier
            .testTag("Account details"),
        text = stringResource(R.string.settings_your_account_label),
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "",
                tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                modifier = Modifier
                    .defaultMinSize(dimensions().wireIconButtonSize)
                    .padding(end = dimensions().spacing8x)
            )
        },
        onClick = onAccountDetailsClick,
    )
}

@Composable
private fun LoggingOutDialog(isLoggingOut: Boolean) {
    if (isLoggingOut) {
        ProgressDialog(
            title = stringResource(R.string.user_profile_logging_out_progress),
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = true
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSelfUserProfileScreen() {
    WireTheme {
        SelfUserProfileContent(
            SelfUserProfileState(
                userId = UserId("value", "domain"),
                status = UserAvailabilityStatus.BUSY,
                fullName = "Tester Tost_long_long_long long  long  long  long  long  long ",
                userName = "userName_long_long_long_long_long_long_long_long_long_long",
                teamName = "Best team ever long  long  long  long  long  long  long  long  long ",
                otherAccounts = listOf(
                    OtherAccount(
                        id = UserId("id1", "domain"),
                        fullName = "Other Name",
                        handle = "userName",
                    ),
                    OtherAccount(
                        id = UserId("id2", "domain"),
                        fullName = "New Name",
                        handle = "userName",
                    )
                ),
                statusDialogData = null,
                legalHoldStatus = LegalHoldUIState.Active,
            ),
            isUserInCall = { false },
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PersonalSelfUserProfileScreenPreview() {
    WireTheme {
        SelfUserProfileContent(
            SelfUserProfileState(
                userId = UserId("value", "domain"),
                status = UserAvailabilityStatus.BUSY,
                fullName = "Some User",
                userName = "some-user",
                teamName = null,
                otherAccounts = listOf(
                    OtherAccount(
                        id = UserId("id1", "domain"),
                        fullName = "Other Name",
                        handle = "userName",
                    ),
                    OtherAccount(
                        id = UserId("id2", "domain"),
                        fullName = "New Name",
                        handle = "userName",
                    )
                ),
                statusDialogData = null,
                legalHoldStatus = LegalHoldUIState.Active,
            ),
            isUserInCall = { false }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCurrentSelfUserStatus() {
    WireTheme {
        CurrentSelfUserStatus(
            UserAvailabilityStatus.AVAILABLE,
            onStatusClicked = {},
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewSelfUserProfileTopBar() {
    WireTheme {
        SelfUserProfileTopBar(onCloseClick = {}, onLogoutClick = {})
    }
}
