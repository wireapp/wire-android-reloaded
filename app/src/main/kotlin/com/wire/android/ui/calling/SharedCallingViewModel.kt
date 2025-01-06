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

package com.wire.android.ui.calling

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset
import com.wire.android.ui.calling.model.InCallReaction
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactions
import com.wire.android.util.ExpiringMap
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.withDelayAfterFirst
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.ConversationTypeForCall
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.functional.onSuccess
import com.wire.kalium.logic.util.PlatformView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel(assistedFactory = SharedCallingViewModel.Factory::class)
class SharedCallingViewModel @AssistedInject constructor(
    @Assisted val conversationId: ConversationId,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val observeEstablishedCallWithSortedParticipants: ObserveEstablishedCallWithSortedParticipantsUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val updateVideoState: UpdateVideoStateUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
    private val turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase,
    private val flipToFrontCamera: FlipToFrontCameraUseCase,
    private val flipToBackCamera: FlipToBackCameraUseCase,
    private val observeSpeaker: ObserveSpeakerUseCase,
    private val observeInCallReactionsUseCase: ObserveInCallReactionsUseCase,
    private val sendInCallReactionUseCase: SendInCallReactionUseCase,
    private val getCurrentClientId: ObserveCurrentClientIdUseCase,
    private val callRinger: CallRinger,
    private val uiCallParticipantMapper: UICallParticipantMapper,
    private val userTypeMapper: UserTypeMapper,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    var callState by mutableStateOf(CallState(conversationId))

    var participantsState by mutableStateOf(persistentListOf<UICallParticipant>())

    private val _inCallReactions = Channel<InCallReaction>(
        capacity = 300, // Max reactions to keep in queue
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val inCallReactions = _inCallReactions.receiveAsFlow().withDelayAfterFirst(InCallReactions.reactionsThrottleDelayMs)

    val recentReactions = recentInCallReactionMap()

    init {
        viewModelScope.launch {
            val allCallsSharedFlow = observeEstablishedCallWithSortedParticipants(conversationId)
                .flowOn(dispatchers.default()).shareIn(this, started = SharingStarted.Lazily)

            launch {
                observeConversationDetails(this)
            }
            launch {
                initCallState(allCallsSharedFlow)
            }
            launch {
                observeParticipants(allCallsSharedFlow)
            }
            launch {
                observeOnSpeaker(this)
            }
            launch {
                observeInCallReactions()
            }
        }
    }

    private suspend fun observeConversationDetails(coroutineScope: CoroutineScope) {
        conversationDetails(conversationId = conversationId)
            .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>() // TODO handle StorageFailure
            .map { it.conversationDetails }
            .flowOn(dispatchers.default())
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(1))
            .collect { details ->
                callState = when (details) {
                    is ConversationDetails.Group -> {
                        callState.copy(
                            conversationName = getConversationName(details.conversation.name),
                            conversationTypeForCall = ConversationTypeForCall.Conference,
                            protocolInfo = details.conversation.protocol,
                            mlsVerificationStatus = details.conversation.mlsVerificationStatus,
                            proteusVerificationStatus = details.conversation.proteusVerificationStatus
                        )
                    }

                    is ConversationDetails.OneOne -> {
                        callState.copy(
                            conversationName = getConversationName(details.otherUser.name),
                            avatarAssetId = details.otherUser.completePicture?.let { assetId ->
                                ImageAsset.UserAvatarAsset(assetId)
                            },
                            conversationTypeForCall = ConversationTypeForCall.OneOnOne,
                            membership = userTypeMapper.toMembership(details.otherUser.userType),
                            protocolInfo = details.conversation.protocol,
                            mlsVerificationStatus = details.conversation.mlsVerificationStatus,
                            proteusVerificationStatus = details.conversation.proteusVerificationStatus,
                            accentId = details.otherUser.accentId
                        )
                    }

                    else -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }

    private suspend fun observeOnSpeaker(coroutineScope: CoroutineScope) {
        observeSpeaker()
            .flowOn(dispatchers.default())
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(1))
            .collectLatest {
                callState = callState.copy(isSpeakerOn = it)
            }
    }

    private suspend fun initCallState(sharedFlow: SharedFlow<Call?>) {
        sharedFlow.first()?.let { call ->
            callState = callState.copy(
                callStatus = call.status,
                callerName = call.callerName,
                isCameraOn = call.isCameraOn,
                isCbrEnabled = call.isCbrEnabled && call.conversationType == Conversation.Type.ONE_ON_ONE
            )
        }
    }

    private suspend fun observeParticipants(sharedFlow: SharedFlow<Call?>) {
        combine(
            getCurrentClientId().filterNotNull(),
            sharedFlow.filterNotNull(),
        ) { clientId, call ->
            callState = callState.copy(
                isMuted = call.isMuted,
                callStatus = call.status,
                isCameraOn = call.isCameraOn,
                isCbrEnabled = call.isCbrEnabled && call.conversationType == Conversation.Type.ONE_ON_ONE,
                callerName = call.callerName,
            )
            participantsState = call.participants.map {
                uiCallParticipantMapper.toUICallParticipant(it, clientId)
            }.toPersistentList()
        }.collect()
    }

    fun hangUpCall(onCompleted: () -> Unit) {
        viewModelScope.launch {
            onCompleted()
            endCall(conversationId)
            resetCallConfig()
            callRinger.stop()
        }
    }

    private suspend fun resetCallConfig() {
        // we need to update mute state to false, so if the user re-join the call te mic will will be muted
        muteCall(conversationId, false)
        if (callState.isCameraOn) {
            flipToFrontCamera(conversationId)
        }
        if (callState.isSpeakerOn) {
            turnLoudSpeakerOff()
        }
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            if (callState.isSpeakerOn) {
                appLogger.i("SharedCallingViewModel: turning off speaker..")
                callState = callState.copy(isSpeakerOn = false)
                turnLoudSpeakerOff()
            } else {
                appLogger.i("SharedCallingViewModel: turning on speaker..")
                callState = callState.copy(isSpeakerOn = true)
                turnLoudSpeakerOn()
            }
        }
    }

    fun flipCamera() {
        viewModelScope.launch {
            if (callState.isOnFrontCamera) {
                appLogger.i("SharedCallingViewModel: flipping to back facing camera..")
                callState = callState.copy(isOnFrontCamera = false)
                flipToBackCamera(conversationId)
            } else {
                appLogger.i("SharedCallingViewModel: flipping to front facing camera..")
                callState = callState.copy(isOnFrontCamera = true)
                flipToFrontCamera(conversationId)
            }
        }
    }

    fun toggleMute(isOnPreviewScreen: Boolean = false) {
        viewModelScope.launch {
            callState.isMuted?.let {
                if (it) {
                    appLogger.i("SharedCallingViewModel: un-muting call..")
                    callState = callState.copy(isMuted = false)
                    unMuteCall(conversationId, !isOnPreviewScreen)
                } else {
                    appLogger.i("SharedCallingViewModel: muting call..")
                    callState = callState.copy(isMuted = true)
                    muteCall(conversationId, !isOnPreviewScreen)
                }
            }
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            appLogger.i("SharedCallingViewModel: toggling video to ${!callState.isCameraOn}..")
            callState = callState.copy(
                isCameraOn = !callState.isCameraOn
            )
            if (callState.isCameraOn) {
                updateVideoState(conversationId, VideoState.STARTED)
            } else {
                updateVideoState(conversationId, VideoState.STOPPED)
            }
        }
    }

    fun clearVideoPreview() {
        viewModelScope.launch {
            appLogger.i("SharedCallingViewModel: clearing video preview..")
            setVideoPreview(conversationId, PlatformView(null))
        }
    }

    fun setVideoPreview(view: View?) {
        viewModelScope.launch(dispatchers.default()) {
            appLogger.i("SharedCallingViewModel: setting video preview..")
            setVideoPreview(conversationId, PlatformView(null))
            setVideoPreview(conversationId, PlatformView(view))
        }
    }

    private suspend fun observeInCallReactions() {
        observeInCallReactionsUseCase(conversationId).collect { message ->

            val sender = participantsState.senderName(message.senderUserId)?.let { name ->
                ReactionSender.Other(name)
            } ?: ReactionSender.Unknown

            message.emojis.forEach { emoji ->
                _inCallReactions.send(InCallReaction(emoji, sender))
            }

            if (message.emojis.isNotEmpty()) {
                recentReactions.put(message.senderUserId, message.emojis.last())
            }
        }
    }

    fun onReactionClick(emoji: String) {
        viewModelScope.launch {
            sendInCallReactionUseCase(conversationId, emoji).onSuccess {
                _inCallReactions.send(InCallReaction(emoji, ReactionSender.You))
            }
        }
    }

    private fun recentInCallReactionMap(): MutableMap<UserId, String> =
        ExpiringMap<UserId, String>(
            scope = viewModelScope,
            expiration = InCallReactions.recentReactionShowDurationMs,
            delegate = mutableStateMapOf<UserId, String>()
        )

    @AssistedFactory
    interface Factory {
        fun create(conversationId: ConversationId): SharedCallingViewModel
    }
}

private fun List<UICallParticipant>.senderName(userId: QualifiedID) = firstOrNull { it.id.value == userId.value }?.name
