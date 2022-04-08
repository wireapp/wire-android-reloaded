package com.wire.android.ui.calling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.calling.controlButtons.DrawCameraButton
import com.wire.android.ui.calling.controlButtons.DrawHangUpButton
import com.wire.android.ui.calling.controlButtons.DrawMicrophoneButton
import com.wire.android.ui.calling.controlButtons.DrawSpeakerButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CallEstablishedScreen(callEstablishedViewModel: CallEstablishedViewModel = hiltViewModel()) {
    CallEstablishedContent(callEstablishedViewModel.callEstablishedState)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CallEstablishedContent(state: CallEstablishedState) {

    val scaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(
        topBar = { CallEstablishedTopBar(state.conversationName) {} },
        sheetShape = RoundedCornerShape(MaterialTheme.wireDimensions.corner16x, MaterialTheme.wireDimensions.corner16x, 0.dp, 0.dp),
        backgroundColor = MaterialTheme.wireColorScheme.callingBackground,
        sheetPeekHeight = MaterialTheme.wireDimensions.defaultSheetPeekHeight,
        scaffoldState = scaffoldState,
        sheetContent = {
            drawCallingControls()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserProfileAvatar(size = MaterialTheme.wireDimensions.callingUserAvatarSize)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallEstablishedTopBar(
    conversationName: String,
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        title = conversationName,
        navigationIconType = NavigationIconType.Collapse,
        elevation = 0.dp,
        actions = {}
    )
}

@Composable
private fun drawCallingControls() {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, MaterialTheme.wireDimensions.spacing16x, 0.dp, 0.dp)
    ) {
        DrawMicrophoneButton()
        DrawCameraButton()
        DrawSpeakerButton()
        DrawHangUpButton()
    }
}


@Preview
@Composable
fun ComposablePreview() {
    CallEstablishedTopBar("Default") {}
}
