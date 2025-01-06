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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.EndCallOnConversationChangeUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class CallsModule {

    @ViewModelScoped
    @Provides
    fun providesCallsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CallsScope = coreLogic.getSessionScope(currentAccount).calls

    @ViewModelScoped
    @Provides
    fun provideGetIncomingCallsUseCase(callsScope: CallsScope) =
        callsScope.getIncomingCalls

    @ViewModelScoped
    @Provides
    fun provideRequestVideoStreamsUseCase(callsScope: CallsScope) =
        callsScope.requestVideoStreams

    @ViewModelScoped
    @Provides
    fun provideIsLastCallClosedUseCase(callsScope: CallsScope) =
        callsScope.isLastCallClosed

    @ViewModelScoped
    @Provides
    fun provideObserveOngoingCallsUseCase(callsScope: CallsScope) =
        callsScope.observeOngoingCalls

    @ViewModelScoped
    @Provides
    fun provideObserveEstablishedCallWithSortedParticipantsUseCase(callsScope: CallsScope) =
        callsScope.observeEstablishedCallWithSortedParticipants

    @ViewModelScoped
    @Provides
    fun provideRejectCallUseCase(callsScope: CallsScope) =
        callsScope.rejectCall

    @ViewModelScoped
    @Provides
    fun provideAcceptCallUseCase(callsScope: CallsScope) =
        callsScope.answerCall

    @ViewModelScoped
    @Provides
    fun provideObserveCallByConversationIdUseCase(
        callsScope: CallsScope
    ): GetAllCallsWithSortedParticipantsUseCase = callsScope.allCallsWithSortedParticipants

    @ViewModelScoped
    @Provides
    fun provideOnGoingCallUseCase(
        callsScope: CallsScope
    ): ObserveEstablishedCallsUseCase =
        callsScope.establishedCall

    @ViewModelScoped
    @Provides
    fun provideObserveOutgoingCallUseCase(
        callsScope: CallsScope
    ): ObserveOutgoingCallUseCase =
        callsScope.observeOutgoingCall

    @ViewModelScoped
    @Provides
    fun provideStartCallUseCase(callsScope: CallsScope): StartCallUseCase =
        callsScope.startCall

    @ViewModelScoped
    @Provides
    fun provideEndCallUseCase(callsScope: CallsScope): EndCallUseCase =
        callsScope.endCall

    @ViewModelScoped
    @Provides
    fun provideEndCallOnConversationChangeUseCase(
        callsScope: CallsScope
    ): EndCallOnConversationChangeUseCase =
        callsScope.endCallOnConversationChange

    @ViewModelScoped
    @Provides
    fun provideMuteCallUseCase(callsScope: CallsScope): MuteCallUseCase =
        callsScope.muteCall

    @ViewModelScoped
    @Provides
    fun provideUnMuteCallUseCase(callsScope: CallsScope): UnMuteCallUseCase =
        callsScope.unMuteCall

    @ViewModelScoped
    @Provides
    fun provideSetVideoPreviewUseCase(
        callsScope: CallsScope
    ): SetVideoPreviewUseCase = callsScope.setVideoPreview

    @ViewModelScoped
    @Provides
    fun provideFlipToBackCameraUseCase(
        callsScope: CallsScope
    ): FlipToBackCameraUseCase = callsScope.flipToBackCamera

    @ViewModelScoped
    @Provides
    fun provideFlipToFrontCameraUseCase(
        callsScope: CallsScope
    ): FlipToFrontCameraUseCase = callsScope.flipToFrontCamera

    @ViewModelScoped
    @Provides
    fun turnLoudSpeakerOffUseCaseProvider(
        callsScope: CallsScope
    ): TurnLoudSpeakerOffUseCase = callsScope.turnLoudSpeakerOff

    @ViewModelScoped
    @Provides
    fun provideTurnLoudSpeakerOnUseCase(
        callsScope: CallsScope
    ): TurnLoudSpeakerOnUseCase = callsScope.turnLoudSpeakerOn

    @ViewModelScoped
    @Provides
    fun provideObserveSpeakerUseCase(
        callsScope: CallsScope
    ): ObserveSpeakerUseCase = callsScope.observeSpeaker

    @ViewModelScoped
    @Provides
    fun provideUpdateVideoStateUseCase(
        callsScope: CallsScope
    ): UpdateVideoStateUseCase =
        callsScope.updateVideoState

    @ViewModelScoped
    @Provides
    fun provideSetVideoSendStateUseCase(
        callsScope: CallsScope
    ): SetVideoSendStateUseCase =
        callsScope.setVideoSendState

    @ViewModelScoped
    @Provides
    fun provideIsCallRunningUseCase(callsScope: CallsScope) =
        callsScope.isCallRunning

    @ViewModelScoped
    @Provides
    fun provideIsEligibleToStartCall(callsScope: CallsScope) =
        callsScope.isEligibleToStartCall

    @ViewModelScoped
    @Provides
    fun provideObserveConferenceCallingEnabledUseCase(callsScope: CallsScope) =
        callsScope.observeConferenceCallingEnabled

    @ViewModelScoped
    @Provides
    fun provideObserveInCallReactionsUseCase(callsScope: CallsScope) =
        callsScope.observeInCallReactions
}
