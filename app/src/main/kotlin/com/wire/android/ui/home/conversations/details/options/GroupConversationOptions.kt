@file:Suppress("TooManyFunctions")
package com.wire.android.ui.home.conversations.details.options

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun GroupConversationOptions(
    viewModel: GroupConversationDetailsViewModel = hiltViewModel(),
    lazyListState: LazyListState
) {
    GroupConversationSettings(
        state = viewModel.groupOptionsState,
        onGuestSwitchClicked = viewModel::onGuestUpdate,
        onServiceSwitchClicked = viewModel::onServicesUpdate,
        lazyListState = lazyListState
    )
    if (viewModel.groupOptionsState.changeGuestOptionConfirmationRequired) {
        DisableGuestConfirmationDialog(
            onConfirm = viewModel::onGuestDialogConfirm,
            onDialogDismiss = viewModel::onGuestDialogDismiss
        )
    }

    if (viewModel.groupOptionsState.changeServiceOptionConfirmationRequired){
        DisableServicesConfirmationDialog(
            onConfirm = viewModel::onServiceDialogConfirm,
            onDialogDismiss = viewModel::onServiceDialogDismiss
        )
    }
}

@Composable
fun GroupConversationSettings(
    state: GroupConversationOptionsState,
    onGuestSwitchClicked: (Boolean) -> Unit,
    onServiceSwitchClicked: (Boolean) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GroupNameItem(groupName = state.groupName, canBeChanged = state.isUpdatingAllowed)
        }
        if (state.areAccessOptionsAvailable) {
            item { FolderHeader(name = stringResource(R.string.folder_lable_access)) }

            item {
                GuestOption(
                    isSwitchEnabled = state.isUpdatingGuestAllowed,
                    isSwitchVisible = state.isUpdatingAllowed,
                    switchState = state.isGuestAllowed,
                    isLoading = state.loadingGuestOption,
                    onCheckedChange = onGuestSwitchClicked
                )
            }

            item {
                ServicesOption(
                    isSwitchEnabledAndVisible = state.isUpdatingAllowed,
                    switchState = state.isServicesAllowed,
                    isLoading = state.loadingServicesOption,
                    onCheckedChange = onServiceSwitchClicked
                )
            }
        }
    }
}

@Composable
private fun GroupNameItem(groupName: String, canBeChanged: Boolean) {
    GroupConversationOptionsItem(
        label = stringResource(id = R.string.conversation_details_options_group_name),
        title = groupName,
        clickable = Clickable(enabled = canBeChanged, onClick = { /* TODO */ }, onLongClick = { /* not handled */ })
    )
    Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
}


@Composable
private fun GuestOption(
    isSwitchEnabled: Boolean,
    isSwitchVisible: Boolean,
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = isSwitchEnabled,
        switchVisible = isSwitchVisible,
        switchState = switchState,
        onClick = onCheckedChange,
        isLoading = isLoading,
        title = R.string.conversation_options_guests_label,
        subTitle = when {
            isSwitchEnabled -> R.string.conversation_options_guest_description
            isSwitchVisible -> R.string.conversation_options_guest_not_editable_description
            else -> null
        }
    )
}

@Composable
private fun ServicesOption(
    isSwitchEnabledAndVisible: Boolean,
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = isSwitchEnabledAndVisible,
        switchVisible = isSwitchEnabledAndVisible,
        switchState = switchState,
        isLoading = isLoading,
        onClick = onCheckedChange,
        title = R.string.conversation_options_services_label,
        subTitle = if (isSwitchEnabledAndVisible) R.string.conversation_options_services_description else null
    )
}

@Composable
private fun GroupOptionWithSwitch(
    switchState: Boolean,
    switchClickable: Boolean,
    switchVisible: Boolean,
    isLoading: Boolean,
    onClick: (Boolean) -> Unit,
    @StringRes title: Int,
    @StringRes subTitle: Int?
) {
    GroupConversationOptionsItem(
        title = stringResource(id = title),
        subtitle = subTitle?.let { stringResource(id = it) },
        switchState = when {
            !switchVisible -> SwitchState.TextOnly(value = switchState)
            switchClickable && !isLoading -> SwitchState.Enabled(value = switchState, onCheckedChange = onClick)
            else -> SwitchState.Disabled(value = switchState)
        },
        arrowType = ArrowType.NONE
    )
    Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
}

@Composable
private fun DisableGuestConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConformationDialog(
        text = R.string.disable_guest_dialog_text,
        title = R.string.disable_guest_dialog_title,
        onConfirm = onConfirm,
        onDismiss = onDialogDismiss
    )
}

@Composable
private fun DisableServicesConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConformationDialog(
        title = R.string.disable_services_dialog_title,
        text = R.string.disable_services_dialog_text,
        onDismiss = onDialogDismiss,
        onConfirm = onConfirm
    )
}

@Composable
private fun DisableConformationDialog(@StringRes title: Int, @StringRes text: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = title),
        text = stringResource(id = text),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(id = R.string.label_disable),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Preview
@Composable
private fun AdminTeamGroupConversationOptionsPreview() {
    GroupConversationSettings(
        GroupConversationOptionsState(
            groupName = "Team Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingAllowed = true,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isUpdatingGuestAllowed = true
        ),
        {}, {}
    )
}

@Preview
@Composable
private fun GuestAdminTeamGroupConversationOptionsPreview() {
    GroupConversationSettings(
        GroupConversationOptionsState(
            groupName = "Team Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingAllowed = true,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isUpdatingGuestAllowed = false
        ),
        {}, {}
    )
}

@Preview
@Composable
private fun MemberTeamGroupConversationOptionsPreview() {
    GroupConversationSettings(
        GroupConversationOptionsState(
            groupName = "Normal Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingAllowed = false,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isUpdatingGuestAllowed = false
        ), {}, {}
    )
}

@Preview
@Composable
private fun NormalGroupConversationOptionsPreview() {
    GroupConversationSettings(
        GroupConversationOptionsState(
            groupName = "Normal Group Conversation",
            areAccessOptionsAvailable = false
        ), {}, {}
    )
}

@Preview(showBackground = true)
@Composable
private fun DisableGuestConformationDialogPreview() {
    DisableGuestConfirmationDialog({}, {})
}

