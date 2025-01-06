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
package com.wire.android.ui.home.messagecomposer.recordaudio

import android.content.Context
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.AudioWavesMaskHelper
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModelTest.Arrangement.Companion.ASSET_SIZE_LIMIT
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCaseImpl
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(CoroutineTestExtension::class)
class RecordAudioViewModelTest {

    @Test
    fun `given user is in a call, when start recording audio, then a info message will be shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withEstablishedCall()
                .withFilterEnabled(false)
                .arrange()

            viewModel.getInfoMessage().test {
                // when
                viewModel.startRecording()

                // then
                val result = awaitItem()
                assertEquals(
                    RecordAudioInfoMessageType.UnableToRecordAudioCall.uiText,
                    result
                )
            }
        }

    @Test
    fun `given user is not in a call, when start recording audio, then recording screen is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.startRecording()

            // then
            assertEquals(
                RecordAudioButtonState.RECORDING,
                viewModel.state.buttonState
            )
            coVerify(exactly = 1) { arrangement.getAssetSizeLimit(false) }
            verify(exactly = 1) { arrangement.audioMediaRecorder.setUp(ASSET_SIZE_LIMIT) }
            verify(exactly = 1) { arrangement.audioMediaRecorder.setUp(ASSET_SIZE_LIMIT) }
            verify(exactly = 1) { arrangement.audioMediaRecorder.startRecording() }
        }

    @Test
    fun `given user is recording audio without filter, when stopping the recording, then send audio button is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.stopRecording()

            // then
            coVerify(exactly = 0) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any()
                )
            }
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.state.buttonState
            )
        }

    @Test
    fun `given user is recording audio with filter, when stopping the recording, then send audio button is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(true)
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.stopRecording()

            // then
            coVerify(exactly = 1) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any(),
                )
            }
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.state.buttonState
            )
        }

    @Test
    fun `given user is recording audio without filter, when applying filter after recording, then effects file is generated`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()
            viewModel.stopRecording()
            assertEquals(null, viewModel.state.effectsOutputFile)
            coVerify(exactly = 0) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any()
                )
            }

            // when
            viewModel.setShouldApplyEffects(true)

            // then
            coVerify(exactly = 1) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any()
                )
            }
            assert(viewModel.state.effectsOutputFile != null)
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.state.buttonState
            )
        }

    @Test
    fun `given user is not recording, when closing audio recording view, then verify that close recording view is called`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.showDiscardRecordingDialog {
                // then
                assertEquals(
                    RecordAudioDialogState.Hidden,
                    viewModel.state.discardDialogState
                )
            }
        }

    @Test
    fun `given user is recording, when closing audio recording view, then discard audio recording dialog is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.showDiscardRecordingDialog {
                // then
                assertEquals(
                    RecordAudioDialogState.Shown,
                    viewModel.state.discardDialogState
                )
            }
        }

    @Test
    fun `given discard audio dialog is shown, when dismissing the dialog, then audio recording dialog is hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.onDismissDiscardDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Hidden,
                viewModel.state.discardDialogState
            )
        }

    @Test
    fun `given user doesn't have audio permissions, when starting to record audio, then permissions dialog is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.showPermissionsDeniedDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Shown,
                viewModel.state.permissionsDeniedDialogState
            )
        }

    @Test
    fun `given permissions dialog is shown, when user dismiss the dialog, then permissions dialog is hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.onDismissPermissionsDeniedDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Hidden,
                viewModel.state.permissionsDeniedDialogState
            )
        }

    @Test
    fun `given user recorded an audio, when discarding the audio, then file is deleted`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()
            viewModel.stopRecording()

            // when
            viewModel.discardRecording {
                // then
                assertEquals(
                    RecordAudioButtonState.ENABLED,
                    viewModel.state.buttonState
                )
                assertEquals(
                    RecordAudioDialogState.Hidden,
                    viewModel.state.discardDialogState
                )
                assertEquals(
                    null,
                    viewModel.state.originalOutputFile
                )
            }
        }

    @Test
    fun `given start recording succeeded, when recording audio, then recording screen is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withStartRecordingSuccessful()
                .withFilterEnabled(false)
                .arrange()

            viewModel.getInfoMessage().test {
                // when
                viewModel.startRecording()
                // then
                assertEquals(RecordAudioButtonState.RECORDING, viewModel.state.buttonState)
                expectNoEvents()
            }
        }

    @Test
    fun `given start recording failed, when recording audio, then info message is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withStartRecordingFailed()
                .withFilterEnabled(false)
                .arrange()

            viewModel.getInfoMessage().test {
                // when
                viewModel.startRecording()
                // then
                assertEquals(RecordAudioButtonState.ENABLED, viewModel.state.buttonState)
                assertEquals(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText, awaitItem())
            }
        }

    private class Arrangement {

        val recordAudioMessagePlayer = mockk<RecordAudioMessagePlayer>()
        val audioMediaRecorder = mockk<AudioMediaRecorder>()
        val observeEstablishedCalls = mockk<ObserveEstablishedCallsUseCase>()
        val currentScreenManager = mockk<CurrentScreenManager>()
        val getAssetSizeLimit = mockk<GetAssetSizeLimitUseCase>()
        val globalDataStore = mockk<GlobalDataStore>()
        val generateAudioFileWithEffects = mockk<GenerateAudioFileWithEffectsUseCase>()
        val context = mockk<Context>()
        val dispatchers = TestDispatcherProvider()
        val fakeKaliumFileSystem = FakeKaliumFileSystem()
        val audioWavesMaskHelper = mockk<AudioWavesMaskHelper>()

        val viewModel by lazy {
            RecordAudioViewModel(
                context = context,
                recordAudioMessagePlayer = recordAudioMessagePlayer,
                observeEstablishedCalls = observeEstablishedCalls,
                currentScreenManager = currentScreenManager,
                audioMediaRecorder = audioMediaRecorder,
                getAssetSizeLimit = getAssetSizeLimit,
                generateAudioFileWithEffects = generateAudioFileWithEffects,
                globalDataStore = globalDataStore,
                dispatchers = dispatchers,
                audioWavesMaskHelper = audioWavesMaskHelper,
                kaliumFileSystem = fakeKaliumFileSystem
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { getAssetSizeLimit.invoke(false) } returns ASSET_SIZE_LIMIT
            every { audioMediaRecorder.setUp(ASSET_SIZE_LIMIT) } returns Unit
            every { audioMediaRecorder.startRecording() } returns true
            every { audioMediaRecorder.stop() } returns Unit
            every { audioMediaRecorder.release() } returns Unit
            coEvery { globalDataStore.setRecordAudioEffectsCheckboxEnabled(any()) } returns Unit
            every { audioMediaRecorder.originalOutputPath } returns fakeKaliumFileSystem
                .tempFilePath("temp_recording.wav")
            coEvery { audioMediaRecorder.getMaxFileSizeReached() } returns flowOf(
                RecordAudioDialogState.MaxFileSizeReached(
                    maxSize = GetAssetSizeLimitUseCaseImpl.ASSET_SIZE_DEFAULT_LIMIT_BYTES
                )
            )
            coEvery { generateAudioFileWithEffects(any(), any(), any()) } returns Unit

            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(
                CurrentScreen.Conversation(id = DUMMY_CALL.conversationId)
            )

            coEvery { recordAudioMessagePlayer.audioMessageStateFlow } returns flowOf(
                AudioState.DEFAULT
            )
            coEvery { recordAudioMessagePlayer.stop() } returns Unit
            coEvery { recordAudioMessagePlayer.close() } returns Unit

            coEvery { observeEstablishedCalls() } returns flowOf(listOf())

            every { audioWavesMaskHelper.getWaveMask(any<File>()) } returns listOf()
            every { audioWavesMaskHelper.getWaveMask(any<Path>()) } returns listOf()
        }

        fun withEstablishedCall() = apply {
            coEvery { observeEstablishedCalls() } returns flowOf(
                listOf(
                    DUMMY_CALL.copy(status = CallStatus.ESTABLISHED)
                )
            )
        }

        fun withStartRecordingSuccessful() = apply { every { audioMediaRecorder.startRecording() } returns true }
        fun withStartRecordingFailed() = apply { every { audioMediaRecorder.startRecording() } returns false }

        fun withFilterEnabled(isEnabled: Boolean) = apply {
            every { globalDataStore.isRecordAudioEffectsCheckboxEnabled() } returns flowOf(isEnabled)
        }

        fun arrange() = this to viewModel

        companion object {
            const val ASSET_SIZE_LIMIT = 5L
            val DUMMY_CALL = Call(
                conversationId = ConversationId(
                    value = "conversationId",
                    domain = "conversationDomain"
                ),
                status = CallStatus.CLOSED,
                callerId = UserId("caller", "domain"),
                participants = listOf(),
                isMuted = true,
                isCameraOn = false,
                isCbrEnabled = false,
                maxParticipants = 0,
                conversationName = "ONE_ON_ONE Name",
                conversationType = Conversation.Type.ONE_ON_ONE,
                callerName = "otherUsername",
                callerTeamName = "team_1"
            )
        }
    }
}
