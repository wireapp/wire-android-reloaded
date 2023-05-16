/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.model

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.SystemMessageItem
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.mock.mockHeader
import com.wire.android.ui.home.conversations.mock.mockMessageWithKnock
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.mock.mockedImageUIMessage
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserId

private val previewUserId = UserId("value", "domain")

@Preview(showBackground = true)
@Composable
fun PreviewMessage() {
    MessageItem(
        message = mockMessageWithText.copy(
            header = mockMessageWithText.header.copy(
                username = UIText.DynamicString(
                    "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                            "Ruiz y Picasso"
                )
            )
        ),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = {},
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageWithReply() {
    MessageItem(
        message = mockMessageWithText.copy(
            header = mockMessageWithText.header.copy(
                username = UIText.DynamicString(
                    "Don Joe"
                )
            ),
            messageContent = UIMessageContent.TextMessage(
                MessageBody(
                    message = UIText.DynamicString("Sure, go ahead!"),
                    quotedMessage = UIQuotedMessage.UIQuotedData(
                        messageId = "asdoij",
                        senderId = previewUserId,
                        senderName = UIText.DynamicString("John Doe"),
                        originalMessageDateDescription = UIText.StringResource(R.string.label_quote_original_message_date, "10:30"),
                        editedTimeDescription = UIText.StringResource(R.string.label_message_status_edited_with_date, "10:32"),
                        quotedContent = UIQuotedMessage.UIQuotedData.Text("Hey, can I call right now?")
                    )
                )
            )
        ),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = {},
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDeletedMessage() {
    MessageItem(
        message = mockMessageWithText.let {
            it.copy(header = it.header.copy(messageStatus = MessageStatus.Deleted))
        },
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = { },
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewFailedMessage() {
    MessageItem(
        message = mockMessageWithText.let {
            it.copy(header = it.header.copy(messageStatus = MessageStatus.SendFailure))
        },
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = { },
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetMessage() {
    MessageItem(
        message = mockAssetMessage(),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = { },
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImportedMediaAssetMessageContent() {
    MessageGenericAsset(
        assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
        assetExtension = "rar.tgz",
        assetSizeInBytes = 99201224L,
        onAssetClick = Clickable(enabled = false),
        assetUploadStatus = Message.UploadStatus.NOT_UPLOADED,
        assetDownloadStatus = Message.DownloadStatus.NOT_DOWNLOADED,
        shouldFillMaxWidth = false,
        isImportedMediaAsset = true
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWideImportedAssetMessageContent() {
    MessageGenericAsset(
        assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
        assetExtension = "rar.tgz",
        assetSizeInBytes = 99201224L,
        onAssetClick = Clickable(enabled = false),
        assetUploadStatus = Message.UploadStatus.NOT_UPLOADED,
        assetDownloadStatus = Message.DownloadStatus.NOT_DOWNLOADED,
        shouldFillMaxWidth = true,
        isImportedMediaAsset = true
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLoadingAssetMessage() {
    MessageGenericAsset(
        assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
        assetExtension = "rar.tgz",
        assetSizeInBytes = 99201224L,
        onAssetClick = Clickable(enabled = false),
        assetUploadStatus = Message.UploadStatus.NOT_UPLOADED,
        assetDownloadStatus = Message.DownloadStatus.DOWNLOAD_IN_PROGRESS,
        shouldFillMaxWidth = true,
        isImportedMediaAsset = false
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewFailedDownloadAssetMessage() {
    MessageGenericAsset(
        assetName = "Some test cool long but very  cool long but very asjkl cool long but very long message",
        assetExtension = "rar.tgz",
        assetSizeInBytes = 99201224L,
        onAssetClick = Clickable(enabled = false),
        assetUploadStatus = Message.UploadStatus.NOT_UPLOADED,
        assetDownloadStatus = Message.DownloadStatus.FAILED_DOWNLOAD,
        shouldFillMaxWidth = true,
        isImportedMediaAsset = false
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageUploaded() {
    MessageItem(
        message = mockedImageUIMessage(Message.UploadStatus.UPLOADED),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = { },
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageUploading() {
    MessageItem(
        message = mockedImageUIMessage(Message.UploadStatus.UPLOAD_IN_PROGRESS),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = { },
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewImageMessageFailedUpload() {
    MessageItem(
        message = mockedImageUIMessage(
            uploadStatus = Message.UploadStatus.FAILED_UPLOAD,
            messageStatus = MessageStatus.SendFailure
        ),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = { },
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMessageWithSystemMessage() {
    Column {
        MessageItem(
            message = mockMessageWithText,
            audioMessagesState = emptyMap(),
            onLongClicked = {},
            onAssetMessageClicked = {},
            onAudioClick = {},
            onChangeAudioPosition = { _, _ -> },
            onImageMessageClicked = { _, _ -> },
            onOpenProfile = { _ -> },
            onReactionClicked = { _, _ -> },
            onResetSessionClicked = { _, _ -> },
            onSelfDeletingMessageRead = { },
            conversationDetailsData = ConversationDetailsData.None
        )
        SystemMessageItem(
            mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MissedCall.YouCalled(
                    UIText.DynamicString("You")
                )
            )
        )
        SystemMessageItem(
            mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MemberAdded(
                    UIText.DynamicString("You"),
                    listOf(UIText.DynamicString("Adam Smith"))
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMessagesWithUnavailableQuotedMessage() {
    MessageItem(
        message = mockMessageWithText.copy(
            messageContent = UIMessageContent.TextMessage(
                MessageBody(
                    message = UIText.DynamicString("Confirmed"),
                    quotedMessage = UIQuotedMessage.UnavailableData
                )
            )
        ),
        audioMessagesState = emptyMap(),
        onLongClicked = {},
        onAssetMessageClicked = {},
        onAudioClick = {},
        onChangeAudioPosition = { _, _ -> },
        onImageMessageClicked = { _, _ -> },
        onOpenProfile = { _ -> },
        onReactionClicked = { _, _ -> },
        onResetSessionClicked = { _, _ -> },
        onSelfDeletingMessageRead = {},
        conversationDetailsData = ConversationDetailsData.None
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAggregatedMessagesWithErrorMessage() {
    Column {
        MessageItem(
            message = mockMessageWithText,
            audioMessagesState = emptyMap(),
            onLongClicked = {},
            onAssetMessageClicked = {},
            onAudioClick = {},
            onChangeAudioPosition = { _, _ -> },
            onImageMessageClicked = { _, _ -> },
            onOpenProfile = { _ -> },
            onReactionClicked = { _, _ -> },
            onResetSessionClicked = { _, _ -> },
            onSelfDeletingMessageRead = {},
            conversationDetailsData = ConversationDetailsData.None
        )
        MessageItem(
            message = mockMessageWithText.copy(
                messageContent = UIMessageContent.TextMessage(
                    MessageBody(
                        message = UIText.DynamicString("Confirmed"),
                        quotedMessage = UIQuotedMessage.UnavailableData
                    )
                )
            ),
            showAuthor = false,
            audioMessagesState = emptyMap(),
            onLongClicked = {},
            onAssetMessageClicked = {},
            onAudioClick = {},
            onChangeAudioPosition = { _, _ -> },
            onImageMessageClicked = { _, _ -> },
            onOpenProfile = { _ -> },
            onReactionClicked = { _, _ -> },
            onResetSessionClicked = { _, _ -> },
            onSelfDeletingMessageRead = {},
            conversationDetailsData = ConversationDetailsData.None
        )
        MessageItem(
            message = mockMessageWithText.copy(
                header = mockHeader.copy(
                    messageStatus = MessageStatus.SendFailure
                )
            ),
            showAuthor = false,
            audioMessagesState = emptyMap(),
            onLongClicked = {},
            onAssetMessageClicked = {},
            onAudioClick = {},
            onChangeAudioPosition = { _, _ -> },
            onImageMessageClicked = { _, _ -> },
            onOpenProfile = { _ -> },
            onReactionClicked = { _, _ -> },
            onResetSessionClicked = { _, _ -> },
            onSelfDeletingMessageRead = {},
            conversationDetailsData = ConversationDetailsData.None
        )
    }
}
