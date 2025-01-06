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

package com.wire.android.ui.home.conversations.details.options

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.collectAsStateLifecycleAware
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import kotlin.time.Duration.Companion.days

@Composable
fun GroupConversationOptions(
    lazyListState: LazyListState,
    onEditGuestAccess: () -> Unit,
    onEditSelfDeletingMessages: () -> Unit,
    viewModel: GroupConversationDetailsViewModel = hiltViewModel(),
    onEditGroupName: () -> Unit
) {
    val state by viewModel.groupOptionsState.collectAsStateLifecycleAware()

    GroupConversationSettings(
        state = state,
        onGuestItemClicked = onEditGuestAccess,
        onSelfDeletingClicked = onEditSelfDeletingMessages,
        onServiceSwitchClicked = viewModel::onServicesUpdate,
        onReadReceiptSwitchClicked = viewModel::onReadReceiptUpdate,
        lazyListState = lazyListState,
        onEditGroupName = onEditGroupName
    )

    if (state.changeServiceOptionConfirmationRequired) {
        DisableServicesConfirmationDialog(
            onConfirm = viewModel::onServiceDialogConfirm,
            onDialogDismiss = viewModel::onServiceDialogDismiss
        )
    }
}

@Composable
fun GroupConversationSettings(
    state: GroupConversationOptionsState,
    onGuestItemClicked: () -> Unit,
    onSelfDeletingClicked: () -> Unit,
    onServiceSwitchClicked: (Boolean) -> Unit,
    onReadReceiptSwitchClicked: (Boolean) -> Unit,
    onEditGroupName: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        item {
            GroupNameItem(
                groupName = state.groupName,
                canBeChanged = state.isUpdatingNameAllowed,
                onClick = onEditGroupName,
            )
        }
        if (state.areAccessOptionsAvailable) {
            item { FolderHeader(name = stringResource(R.string.folder_label_access)) }

            item {
                GroupConversationOptionsItem(
                    title = stringResource(id = R.string.conversation_options_guests_label),
                    subtitle = stringResource(id = R.string.conversation_details_guest_description),
                    switchState = SwitchState.TextOnly(value = state.isGuestAllowed),
                    arrowType = if (state.isUpdatingGuestAllowed) ArrowType.TITLE_ALIGNED else ArrowType.NONE,
                    clickable = Clickable(
                        enabled = state.isUpdatingGuestAllowed,
                        onClick = onGuestItemClicked,
                        onClickDescription = stringResource(id = R.string.content_description_conversation_details_guests_action)
                    ),
                )
            }

            item { WireDivider(color = colorsScheme().divider) }

            item {
                ServicesOption(
                    isSwitchEnabledAndVisible = state.isUpdatingServicesAllowed,
                    switchState = state.isServicesAllowed,
                    isLoading = state.loadingServicesOption,
                    onCheckedChange = onServiceSwitchClicked
                )
            }
        }
        item { FolderHeader(name = stringResource(id = R.string.folder_label_messaging)) }

        if (!state.selfDeletionTimer.isDisabled) {
            item {
                GroupConversationOptionsItem(
                    title = stringResource(id = R.string.conversation_options_self_deleting_messages_label),
                    subtitle = stringResource(id = R.string.conversation_options_self_deleting_messages_description),
                    trailingOnText = if (state.selfDeletionTimer.isEnforced) {
                        "(${state.selfDeletionTimer.duration.toSelfDeletionDuration().shortLabel.asString()})"
                    } else {
                        null
                    },
                    switchState = SwitchState.TextOnly(value = state.selfDeletionTimer.isEnforced),
                    arrowType = if (state.isUpdatingSelfDeletingAllowed && !state.selfDeletionTimer.isEnforcedByTeam) {
                        ArrowType.TITLE_ALIGNED
                    } else {
                        ArrowType.NONE
                    },
                    clickable = Clickable(
                        enabled = state.isUpdatingSelfDeletingAllowed && !state.selfDeletionTimer.isEnforcedByTeam,
                        onClick = onSelfDeletingClicked,
                        onClickDescription = stringResource(id = R.string.content_description_conversation_details_self_deleting_action)
                    )
                )
            }
        }
        item { WireDivider(color = colorsScheme().divider) }
        item {
            ReadReceiptOption(
                isSwitchEnabled = state.isUpdatingReadReceiptAllowed,
                switchState = state.isReadReceiptAllowed,
                isLoading = state.loadingReadReceiptOption,
                onCheckedChange = onReadReceiptSwitchClicked
            )
        }
        item {
            ConversationProtocolDetails(
                protocolInfo = state.protocolInfo
            )
        }
    }
}

@Composable
fun ConversationProtocolDetails(
    protocolInfo: Conversation.ProtocolInfo,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        FolderHeader(name = stringResource(R.string.folder_label_protocol_details))

        ProtocolDetails(
            label = UIText.StringResource(R.string.protocol),
            text = UIText.DynamicString(protocolInfo.name())
        )

        if (protocolInfo is Conversation.ProtocolInfo.MLS) {
            ProtocolDetails(
                label = UIText.StringResource(R.string.cipher_suite),
                text = UIText.DynamicString(protocolInfo.cipherSuite.toString())
            )

            if (BuildConfig.PRIVATE_BUILD) {
                ProtocolDetails(
                    label = UIText.StringResource(R.string.last_key_material_update_label),
                    text = UIText.DynamicString(protocolInfo.keyingMaterialLastUpdate.toString())
                )

                ProtocolDetails(
                    label = UIText.StringResource(R.string.group_state_label),
                    text = UIText.DynamicString(protocolInfo.groupState.name)
                )
            }
        }
    }
}

@Composable
private fun GroupNameItem(
    groupName: String,
    canBeChanged: Boolean,
    onClick: () -> Unit = {},
) {
    GroupConversationOptionsItem(
        label = stringResource(id = R.string.conversation_details_options_group_name),
        title = groupName,
        clickable = Clickable(
            enabled = canBeChanged,
            onClick = onClick,
            onClickDescription = stringResource(id = R.string.content_description_edit_label)
        ),
        arrowType = if (!canBeChanged) ArrowType.NONE else ArrowType.CENTER_ALIGNED,
    )
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
}

@Composable
private fun ProtocolDetails(label: UIText, text: UIText) {
    GroupConversationOptionsItem(
        label = label.asString(),
        title = text.asString(),
        arrowType = ArrowType.NONE
    )
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
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
private fun ReadReceiptOption(
    isSwitchEnabled: Boolean,
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = isSwitchEnabled,
        switchVisible = isSwitchEnabled,
        switchState = switchState,
        isLoading = isLoading,
        onClick = onCheckedChange,
        title = R.string.conversation_options_read_receipt_label,
        subTitle = R.string.conversation_options_read_receipt_description
    )
}

@Composable
fun GroupOptionWithSwitch(
    switchState: Boolean,
    switchClickable: Boolean,
    switchVisible: Boolean,
    isLoading: Boolean,
    onClick: (Boolean) -> Unit,
    @StringRes title: Int,
    @StringRes subTitle: Int?,
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
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
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
fun DisableConformationDialog(@StringRes title: Int, @StringRes text: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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

@PreviewMultipleThemes
@Composable
fun PreviewAdminTeamGroupConversationOptions() = WireTheme {
    GroupConversationSettings(
        state = GroupConversationOptionsState(
            conversationId = ConversationId("someValue", "someDomain"),
            groupName = "Team Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingNameAllowed = true,
            isUpdatingGuestAllowed = true,
            isUpdatingServicesAllowed = true,
            isUpdatingSelfDeletingAllowed = true,
            isUpdatingReadReceiptAllowed = true,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isReadReceiptAllowed = true,
            mlsEnabled = true
        ),
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onEditGroupName = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGuestAdminTeamGroupConversationOptions() = WireTheme {
    GroupConversationSettings(
        state = GroupConversationOptionsState(
            conversationId = ConversationId("someValue", "someDomain"),
            groupName = "Team Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingNameAllowed = true,
            isUpdatingGuestAllowed = false,
            isUpdatingServicesAllowed = true,
            isUpdatingSelfDeletingAllowed = true,
            isUpdatingReadReceiptAllowed = true,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isReadReceiptAllowed = true,
        ),
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onEditGroupName = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewExternalMemberAdminTeamGroupConversationOptions() = WireTheme {
    GroupConversationSettings(
        state = GroupConversationOptionsState(
            conversationId = ConversationId("someValue", "someDomain"),
            groupName = "Team Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingNameAllowed = false,
            isUpdatingGuestAllowed = false,
            isUpdatingServicesAllowed = true,
            isUpdatingSelfDeletingAllowed = true,
            isUpdatingReadReceiptAllowed = true,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isReadReceiptAllowed = true,
        ),
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onEditGroupName = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMemberTeamGroupConversationOptions() = WireTheme {
    GroupConversationSettings(
        state = GroupConversationOptionsState(
            conversationId = ConversationId("someValue", "someDomain"),
            groupName = "Normal Group Conversation",
            areAccessOptionsAvailable = true,
            isUpdatingNameAllowed = false,
            isUpdatingGuestAllowed = false,
            isUpdatingServicesAllowed = false,
            isUpdatingSelfDeletingAllowed = false,
            isUpdatingReadReceiptAllowed = false,
            isGuestAllowed = true,
            isServicesAllowed = true,
            isReadReceiptAllowed = true,
        ),
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onEditGroupName = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewNormalGroupConversationOptions() = WireTheme {
    GroupConversationSettings(
        state = GroupConversationOptionsState(
            conversationId = ConversationId("someValue", "someDomain"),
            groupName = "Normal Group Conversation",
            areAccessOptionsAvailable = false
        ),
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onEditGroupName = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewNormalGroupConversationOptionsWithSelfDelet() = WireTheme {
    GroupConversationSettings(
        state = GroupConversationOptionsState(
            conversationId = ConversationId("someValue", "someDomain"),
            groupName = "Normal Group Conversation",
            areAccessOptionsAvailable = false,
            selfDeletionTimer = SelfDeletionTimer.Enabled(3.days),
            isUpdatingSelfDeletingAllowed = true
        ),
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onEditGroupName = {},
    )
}
