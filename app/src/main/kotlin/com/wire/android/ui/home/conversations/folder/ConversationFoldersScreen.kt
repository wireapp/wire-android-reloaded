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
package com.wire.android.ui.home.conversations.folder

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination

@RootNavGraph
@WireDestination(
    navArgsDelegate = ConversationFoldersNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ConversationFoldersScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<ConversationFoldersNavBackArgs>
) {
    val args = remember {
        navigator.navController.currentBackStackEntry?.let {
            ConversationFoldersScreenDestination.argsFrom(it)
        }
    }

    Content(
        args = args!!,
        onNavigationPressed = { navigator.navigateBack() },
        onMoveToFolderMessage = { message ->
            resultNavigator.setResult(ConversationFoldersNavBackArgs(message = message))
            resultNavigator.navigateBack()
        }
    )
}

@Composable
private fun Content(
    args: ConversationFoldersNavArgs,
    onNavigationPressed: () -> Unit = {},
    onMoveToFolderMessage: (String) -> Unit = {},
    foldersViewModel: ConversationFoldersVM =
        hiltViewModel<ConversationFoldersVMImpl, ConversationFoldersVMImpl.Factory>(
            creationCallback = { it.create(ConversationFoldersStateArgs(args.currentFolderId)) }
        ),
    moveToFolderVM: MoveConversationToFolderVM =
        hiltViewModel<MoveConversationToFolderVMImpl, MoveConversationToFolderVMImpl.Factory>(
            creationCallback = {
                it.create(MoveConversationToFolderArgs(args.conversationId, args.conversationName, args.currentFolderId))
            }
        )
) {
    val resources = LocalContext.current.resources
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        moveToFolderVM.infoMessage.collect {
            onMoveToFolderMessage(it.asString(resources))
        }
    }

    val lazyListState = rememberLazyListState()
    WireScaffold(
        modifier = Modifier
            .background(color = colorsScheme().background),

        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.label_move_to_folder),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onNavigationPressed,
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(dimensions().spacing16x)) {
                WireSecondaryButton(
                    state = WireButtonState.Default,
                    text = stringResource(id = R.string.label_new_folder),
                    onClick = {
                        Toast.makeText(
                            context,
                            "Not implemented yet",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                VerticalSpace.x8()
                val state = if (foldersViewModel.state().selectedFolderId != null
                    && foldersViewModel.state().selectedFolderId != args.currentFolderId
                ) {
                    WireButtonState.Default
                } else {
                    WireButtonState.Disabled
                }
                WireButton(
                    state = state,
                    text = stringResource(id = R.string.label_done),
                    onClick = {
                        moveToFolderVM.moveConversationToFolder(
                            foldersViewModel.state().folders.first { it.id == foldersViewModel.state().selectedFolderId!! }
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (foldersViewModel.state().folders.isEmpty()) {
                Text(
                    stringResource(R.string.folder_create_description),
                    modifier = Modifier.align(Alignment.Center),
                    style = typography().body01,
                    color = colorsScheme().secondaryText
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(foldersViewModel.state().folders) { folder ->
                        val state = if (foldersViewModel.state().selectedFolderId == folder.id) {
                            RichMenuItemState.SELECTED
                        } else {
                            RichMenuItemState.DEFAULT
                        }
                        SelectableMenuBottomSheetItem(
                            title = folder.name,
                            onItemClick = Clickable(
                                enabled = state == RichMenuItemState.DEFAULT,
                                onClickDescription = stringResource(id = R.string.content_description_select_label),
                                onClick = { foldersViewModel.onFolderSelected(folder.id) }
                            ),
                            state = state,
                            modifier = Modifier.height(dimensions().spacing48x)
                        )
                    }
                }
            }
        }
    }
}