package com.wire.android.ui.common.groupname

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.ShakeAnimation
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.groupname.GroupNameMode.CREATION
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.ConversationOptions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GroupNameScreen(
    newGroupState: GroupMetadataState,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit,
) {
    with(newGroupState) {
        val scrollState = rememberScrollState()

        Scaffold(topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = onBackPressed,
                title = stringResource(id = if (mode == CREATION) R.string.new_group_title else R.string.group_name_title)
            )
        }) { internalPadding ->

            Column(
                modifier = Modifier
                    .padding(internalPadding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    Text(
                        text = stringResource(id = R.string.group_name_description),
                        style = MaterialTheme.wireTypography.body01,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.wireDimensions.spacing16x,
                                vertical = MaterialTheme.wireDimensions.spacing24x
                            )
                    )
                    Box {
                        ShakeAnimation { animate ->
                            if (animatedGroupNameError) {
                                animate()
                                onGroupNameErrorAnimated()
                            }
                            WireTextField(
                                value = groupName,
                                onValueChange = onGroupNameChange,
                                placeholderText = stringResource(R.string.group_name_placeholder),
                                labelText = stringResource(R.string.group_name_title).uppercase(),
                                state = computeGroupMetadataState(error),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                            )
                        }
                    }
                    if (mode == CREATION && mlsEnabled) {
                        WireDropDown(
                            items =
                            ConversationOptions.Protocol.values().map { it.name },
                            defaultItemIndex = 0,
                            stringResource(R.string.protocol),
                            modifier = Modifier
                                .padding(MaterialTheme.wireDimensions.spacing16x)
                        ) { selectedIndex ->
                            groupProtocol = ConversationOptions.Protocol.values()[selectedIndex]
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }

                Surface(
                    shadowElevation = scrollState.rememberBottomBarElevationState().value,
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                        WirePrimaryButton(
                            text = stringResource(if (mode == CREATION) R.string.label_continue else R.string.label_ok),
                            onClick = onContinuePressed,
                            fillMaxWidth = true,
                            loading = isLoading,
                            trailingIcon = Icons.Filled.ChevronRight.Icon(),
                            state = if (continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun computeGroupMetadataState(error: GroupMetadataState.NewGroupError) =
    if (error is GroupMetadataState.NewGroupError.TextFieldError) when (error) {
        GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError ->
            WireTextFieldState.Error(stringResource(id = R.string.empty_group_name_error))
        GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError ->
            WireTextFieldState.Error(stringResource(id = R.string.group_name_exceeded_limit_error))
    } else {
        WireTextFieldState.Default
    }

@Preview
@Composable
private fun GroupNameScreenEditPreview() {
    GroupNameScreen(
        GroupMetadataState(groupName = TextFieldValue("group name")),
        onGroupNameChange = {},
        onContinuePressed = {},
        onGroupNameErrorAnimated = {},
        onBackPressed = {}
    )
}
