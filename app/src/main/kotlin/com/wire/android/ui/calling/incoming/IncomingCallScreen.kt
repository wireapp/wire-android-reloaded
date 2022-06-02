package com.wire.android.ui.calling.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlButtons.AcceptButton
import com.wire.android.ui.calling.controlButtons.CallOptionsControls
import com.wire.android.ui.calling.controlButtons.DeclineButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun IncomingCallScreen(
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
    incomingCallViewModel: IncomingCallViewModel = hiltViewModel()
) {
    val audioPermissionCheck = AudioBluetoothPermissionCheckFlow(incomingCallViewModel = incomingCallViewModel)

    IncomingCallContent(
        state = sharedCallingViewModel.callState,
        toggleMute = sharedCallingViewModel::toggleMute,
        declineCall = {
            incomingCallViewModel.declineCall()
        },
        acceptCall = {
            audioPermissionCheck.launch()
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun IncomingCallContent(
    state: CallState,
    toggleMute: () -> Unit,
    declineCall: () -> Unit,
    acceptCall: () -> Unit
) {

    val scaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(
        topBar = { IncomingCallTopBar { } },
        sheetShape = RoundedCornerShape(dimensions().corner16x, dimensions().corner16x, 0.dp, 0.dp),
        backgroundColor = MaterialTheme.wireColorScheme.callingIncomingBackground,
        sheetGesturesEnabled = false,
        scaffoldState = scaffoldState,
        sheetPeekHeight = dimensions().defaultIncomingCallSheetPeekHeight,
        sheetContent = {
            CallingControls(
                callState =  state,
                toggleMute = toggleMute,
                declineCall = declineCall,
                acceptCall = acceptCall
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (state.conversationName) {
                    is ConversationName.Known -> state.conversationName.name
                    is ConversationName.Unknown -> stringResource(id = state.conversationName.resourceId)
                    else -> ""
                },
                style = MaterialTheme.wireTypography.title01,
                modifier = Modifier.padding(top = dimensions().spacing24x)
            )
            Text(
                text = stringResource(id = R.string.calling_label_incoming_call),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(top = dimensions().spacing8x)
            )
            UserProfileAvatar(
                userAvatarAsset = state.avatarAssetId,
                size = dimensions().callingIncomingUserAvatarSize,
                modifier = Modifier.padding(top = dimensions().spacing56x)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingCallTopBar(
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        title = stringResource(id = R.string.calling_label_constant_bit_rate),
        titleStyle = MaterialTheme.wireTypography.title03,
        navigationIconType = NavigationIconType.Collapse,
        elevation = 0.dp,
        actions = {}
    )
}

@Composable
private fun CallingControls(
    callState: CallState,
    toggleMute: () -> Unit,
    declineCall: () -> Unit,
    acceptCall: () -> Unit
) {
    CallOptionsControls(callState, toggleMute)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensions().spacing40x,
                top = dimensions().spacing32x,
                end = dimensions().spacing40x
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(alignment = Alignment.CenterStart)
        ) {
            DeclineButton { declineCall() }
            Text(
                text = stringResource(id = R.string.calling_label_decline),
                style = MaterialTheme.wireTypography.body03,
                modifier = Modifier.padding(
                    top = dimensions().spacing8x,
                    bottom = dimensions().spacing40x
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
        ) {
            AcceptButton { acceptCall() }
            Text(
                text = stringResource(id = R.string.calling_label_accept),
                style = MaterialTheme.wireTypography.body03,
                modifier = Modifier.padding(
                    top = dimensions().spacing8x,
                    bottom = dimensions().spacing40x
                )
            )
        }
    }
}

@Composable
private fun AudioBluetoothPermissionCheckFlow(incomingCallViewModel: IncomingCallViewModel) =
    rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
        appLogger.d("IncomingCall - Permissions granted")
        incomingCallViewModel.acceptCall()
    }) {
        appLogger.d("IncomingCall - Permissions denied")
        incomingCallViewModel.declineCall()
    }

@Preview
@Composable
fun ComposablePreview() {
    IncomingCallScreen()
}
