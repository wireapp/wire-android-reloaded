package com.wire.android.ui.userprofile.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    viewModel: ServiceDetailsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            Column {
                ServiceDetailsTopAppBar(
                    onBackPressed = viewModel::navigateBack
                )
            }
        },
        content = { internalPadding ->
            Column(modifier = Modifier.padding(internalPadding)) {
                ServiceDetailsProfileInfo(state = viewModel.serviceDetailsState)
                ServiceDetailsDescription(state = viewModel.serviceDetailsState)
            }
        },
        bottomBar = {
            ServiceDetailsAddOrRemoveButton(
                buttonState = viewModel.serviceDetailsState.buttonState,
                addService = viewModel::addService,
                removeService = viewModel::removeService
            )
        }
    )

}

@Composable
private fun ServiceDetailsTopAppBar(
    onBackPressed: () -> Unit,
) {
    WireCenterAlignedTopAppBar(
        elevation = dimensions().spacing0x,
        title = stringResource(id = R.string.service_details_label),
        onNavigationPressed = onBackPressed
    )
}

@Composable
private fun ServiceDetailsProfileInfo(
    state: ServiceDetailsState
) {
    state.serviceDetails?.let { serviceDetails ->
        UserProfileInfo(
            isLoading = state.isAvatarLoading,
            avatarAsset = state.serviceAvatarAsset,
            fullName = serviceDetails.name,
            userName = "",
            teamName = null,
            membership = Membership.Service,
            editableState = EditableState.NotEditable,
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
            securityClassificationType = SecurityClassificationType.NONE
        )
    }
}

@Composable
private fun ServiceDetailsDescription(
    state: ServiceDetailsState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing16x))
        Text(
            // modifier = Modifier.fillMaxWidth(),
            text = state.serviceDetails?.description ?: "",
            style = MaterialTheme.wireTypography.body01
        )
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing24x))
        Text(
            text = state.serviceDetails?.summary ?: "",
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = dimensions().spacing18x)
                // .fillMaxWidth()
        )
    }
}

@Composable
private fun ServiceDetailsAddOrRemoveButton(
    buttonState: ServiceDetailsButtonState,
    addService: () -> Unit,
    removeService: () -> Unit
) {
    val (shouldShow: Boolean, textString: String?) = when (buttonState) {
        ServiceDetailsButtonState.HIDDEN -> Pair(false, null)
        ServiceDetailsButtonState.ADD -> Pair(true, stringResource(id = R.string.service_details_add_service_label))
        ServiceDetailsButtonState.REMOVE -> Pair(true, stringResource(id = R.string.service_details_remove_service_label))
    }
    if (shouldShow) {
        Surface(
            color = MaterialTheme.wireColorScheme.background,
            shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
        ) {
            Divider(color = colorsScheme().outline)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WirePrimaryButton(
                    text = textString,
                    onClick = if (buttonState == ServiceDetailsButtonState.ADD) addService else removeService,
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                    modifier = Modifier
                        .weight(1f)
                        .padding(dimensions().spacing16x)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewServiceDetailsScreen() {
    ServiceDetailsScreen()
}
