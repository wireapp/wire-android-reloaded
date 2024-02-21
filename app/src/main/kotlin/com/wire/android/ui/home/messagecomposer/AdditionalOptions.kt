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

package com.wire.android.ui.home.messagecomposer

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment
import androidx.viewbinding.ViewBinding
import com.wire.android.feature.sketch.DrawingFragment
import com.wire.android.feature.sketch.databinding.FragmentContainerBinding
import com.wire.android.feature.sketch.databinding.FragmentDrawingBinding
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.location.GeoLocatedAddress
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioComponent
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.RichTextMarkdown
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.permission.PermissionDenialType

@Composable
fun AdditionalOptionsMenu(
    additionalOptionsState: AdditionalOptionMenuState,
    selectedOption: AdditionalOptionSelectItem,
    isSelfDeletingSettingEnabled: Boolean,
    isSelfDeletingActive: Boolean,
    isEditing: Boolean,
    isMentionActive: Boolean,
    onOnSelfDeletingOptionClicked: (() -> Unit)? = null,
    onAdditionalOptionsMenuClicked: () -> Unit,
    onMentionButtonClicked: (() -> Unit),
    onGifOptionClicked: (() -> Unit)? = null,
    onPingOptionClicked: () -> Unit,
    onRichEditingButtonClicked: () -> Unit,
    onCloseRichEditingButtonClicked: () -> Unit,
    onRichOptionButtonClicked: (RichTextMarkdown) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(colorsScheme().messageComposerBackgroundColor)) {
        when (additionalOptionsState) {
            AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu -> {
                AttachmentAndAdditionalOptionsMenuItems(
                    selectedOption = selectedOption,
                    isEditing = isEditing,
                    isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
                    isSelfDeletingActive = isSelfDeletingActive,
                    isMentionActive = isMentionActive,
                    onMentionButtonClicked = onMentionButtonClicked,
                    onAdditionalOptionsMenuClicked = onAdditionalOptionsMenuClicked,
                    onGifButtonClicked = onGifOptionClicked ?: {},
                    onSelfDeletionOptionButtonClicked = onOnSelfDeletingOptionClicked ?: {},
                    onRichEditingButtonClicked = onRichEditingButtonClicked,
                    onPingClicked = onPingOptionClicked
                )
            }

            AdditionalOptionMenuState.RichTextEditing -> {
                RichTextOptions(
                    onRichTextHeaderButtonClicked = { onRichOptionButtonClicked(RichTextMarkdown.Header) },
                    onRichTextBoldButtonClicked = { onRichOptionButtonClicked(RichTextMarkdown.Bold) },
                    onRichTextItalicButtonClicked = { onRichOptionButtonClicked(RichTextMarkdown.Italic) },
                    onCloseRichTextEditingButtonClicked = onCloseRichEditingButtonClicked
                )
            }

            AdditionalOptionMenuState.Hidden -> {}
        }
    }
}

@Composable
fun AdditionalOptionSubMenu(
    isFileSharingEnabled: Boolean,
    onCaptureVideoPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    onLocationPickerClicked: () -> Unit,
    onCloseAdditionalAttachment: () -> Unit,
    onRecordAudioMessageClicked: () -> Unit,
    additionalOptionsState: AdditionalOptionSubMenuState,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onLocationPicked: (GeoLocatedAddress) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        AttachmentOptionsComponent(
            onAttachmentPicked = onAttachmentPicked,
            tempWritableImageUri = tempWritableImageUri,
            tempWritableVideoUri = tempWritableVideoUri,
            isFileSharingEnabled = isFileSharingEnabled,
            onRecordAudioMessageClicked = onRecordAudioMessageClicked,
            onLocationPickerClicked = onLocationPickerClicked,
            onCaptureVideoPermissionPermanentlyDenied = onCaptureVideoPermissionPermanentlyDenied
        )
        when (additionalOptionsState) {
            AdditionalOptionSubMenuState.AttachFile -> {
                /* DO NOTHING, ALREADY DISPLAYED AS PARENT */
            }

            AdditionalOptionSubMenuState.RecordAudio -> {
                RecordAudioComponent(
                    onAudioRecorded = onAudioRecorded,
                    onCloseRecordAudio = onCloseAdditionalAttachment
                )
            }

            AdditionalOptionSubMenuState.Location -> {
                FragmentHolderScreen(
                    androidViewBindingFactory = FragmentContainerBinding::inflate,
                    androidViewBindingUpdate = {
                        with(fragmentContainerView.findFragment<DrawingFragment>()) {
                            // Reference of AnalyticsEventsFragment is available here
                        }
                    },
                    onBackPress = {

                    },
                )
            }
            // non functional for now
            AdditionalOptionSubMenuState.AttachImage -> {}
            AdditionalOptionSubMenuState.Emoji -> {}
            AdditionalOptionSubMenuState.Gif -> {}
        }
    }
}

@Composable
fun AttachmentAndAdditionalOptionsMenuItems(
    isEditing: Boolean,
    selectedOption: AdditionalOptionSelectItem,
    isMentionActive: Boolean,
    onMentionButtonClicked: () -> Unit,
    onAdditionalOptionsMenuClicked: () -> Unit = {},
    onPingClicked: () -> Unit = {},
    onSelfDeletionOptionButtonClicked: () -> Unit,
    isSelfDeletingSettingEnabled: Boolean,
    isSelfDeletingActive: Boolean,
    onGifButtonClicked: () -> Unit = {},
    onRichEditingButtonClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        MessageComposeActions(
            isEditing = isEditing,
            selectedOption = selectedOption,
            isMentionActive = isMentionActive,
            onMentionButtonClicked = onMentionButtonClicked,
            onAdditionalOptionButtonClicked = onAdditionalOptionsMenuClicked,
            onPingButtonClicked = onPingClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
            isSelfDeletingActive = isSelfDeletingActive,
            onGifButtonClicked = onGifButtonClicked,
            onRichEditingButtonClicked = onRichEditingButtonClicked
        )
    }
}

@Composable
fun FragmentContainer(
    modifier: Modifier = Modifier,
    fragmentManager: FragmentManager,
    commit: FragmentTransaction.(containerId: Int) -> Unit
) {
    val containerId by rememberSaveable { mutableStateOf(View.generateViewId()) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            fragmentManager.findFragmentById(containerId)?.view
                ?.also { (it.parent as? ViewGroup)?.removeView(it) }
                ?: FragmentContainerView(context)
                    .apply { id = containerId }
                    .also {
                        fragmentManager.commit { commit(it.id) }
                    }
        },
        update = {}
    )
}

@Composable
fun <T : ViewBinding> FragmentHolderScreen(
    androidViewBindingFactory: (inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) -> T,
    onBackPress: () -> Unit = {},
    androidViewBindingUpdate: T.() -> Unit = {},
) {
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = "",
            navigationIconType = NavigationIconType.Close,
            onNavigationPressed = onBackPress
        )
//        } else {
//            Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
//        }
    }) { paddingValues ->
        AndroidViewBinding(
            factory = androidViewBindingFactory,
            modifier = Modifier.padding(paddingValues),
            update = androidViewBindingUpdate,
        )
    }
}
