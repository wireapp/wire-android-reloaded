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
package com.wire.android.media

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.AudioWavesMaskHelper
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer.MessageIdWrapper
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

@Suppress("LongMethod")
class ConversationAudioMessagePlayerTest {

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingAudioForFirstTime_thenEmitStatesAsExpected() = runTest {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement()
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessFullAssetFetch()
            .withCurrentSession()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, testAudioMessageId)

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // skip first emit from onStart
            awaitItem()
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId
            )
            this@runTest.advanceUntilIdle()

            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)

                val totalTime = currentState!!.totalTimeInMs
                assert(totalTime is AudioState.TotalTimeInMs.Known)
                assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
            }

            cancelAndIgnoreRemainingEvents()
        }

        with(arrangement) {
            verify { mediaPlayer.prepare() }
            verify { mediaPlayer.setDataSource(any(), any()) }
            verify { mediaPlayer.start() }

            verify(exactly = 1) { audioFocusHelper.request() }
            verify(exactly = 1) { servicesManager.startPlayingAudioMessageService() }

            verify(exactly = 0) { mediaPlayer.seekTo(any()) }
        }
    }

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingTheSameMessageIdTwiceSequentially_thenEmitStatesAsExpected() = runTest {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement()
            .withSuccessFullAssetFetch()
            .withCurrentSession()
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withMediaPlayerPlaying()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val messageIdWrapper = MessageIdWrapper(conversationId, testAudioMessageId)

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // skip first emit from onStart
            awaitItem()
            // playing first time
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId
            )
            advanceUntilIdle()

            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
            }
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
            }
            awaitItem() // currentPosition update
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)

                val totalTime = currentState!!.totalTimeInMs
                assert(totalTime is AudioState.TotalTimeInMs.Known)
                assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
            }

            // playing second time
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId
            )
            advanceUntilIdle()
            awaitAndAssertStateUpdate { state ->
                val currentState = state[messageIdWrapper]
                assert(currentState != null)
                assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Paused)
            }

            cancelAndIgnoreRemainingEvents()
        }

        with(arrangement) {
            verify(exactly = 1) { mediaPlayer.prepare() }
            verify(exactly = 1) { mediaPlayer.setDataSource(any(), any()) }
            verify(exactly = 1) { mediaPlayer.start() }
            verify(exactly = 1) { mediaPlayer.pause() }

            verify(exactly = 0) { mediaPlayer.seekTo(any()) }
        }
    }

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayed_thenEmitStatesAsExpected() =
        runTest {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement()
                .withSuccessFullAssetFetch()
                .withCurrentSession()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .arrange()

            val firstAudioMessageId = "some-dummy-message-id1"
            val secondAudioMessageId = "some-dummy-message-id2"
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val firstAudioMessageIdWrapper = MessageIdWrapper(conversationId, firstAudioMessageId)
            val secondAudioMessageIdWrapper = MessageIdWrapper(conversationId, secondAudioMessageId)

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // skip first emit from onStart
                awaitItem()
                // playing first audio message
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    firstAudioMessageId
                )
                this@runTest.advanceUntilIdle()

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second audio message
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    secondAudioMessageId
                )
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Completed)
                }
                awaitItem() // seekToAudioPosition
                awaitAndAssertStateUpdate { state ->
                    assertEquals(2, state.size)

                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                cancelAndIgnoreRemainingEvents()
            }

            with(arrangement) {
                verify(exactly = 2) { mediaPlayer.prepare() }
                verify(exactly = 2) { mediaPlayer.setDataSource(any(), any()) }
                verify(exactly = 2) { mediaPlayer.start() }

                verify(exactly = 0) { mediaPlayer.seekTo(any()) }
            }
        }

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayedAndSecondResumed_thenEmitStatesAsExpected() =
        runTest {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement()
                .withSuccessFullAssetFetch()
                .withCurrentSession()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .arrange()

            val firstAudioMessageId = "some-dummy-message-id1"
            val secondAudioMessageId = "some-dummy-message-id2"
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val firstAudioMessageIdWrapper = MessageIdWrapper(conversationId, firstAudioMessageId)
            val secondAudioMessageIdWrapper = MessageIdWrapper(conversationId, secondAudioMessageId)

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // skip first emit from onStart
                awaitItem()
                // playing first audio message
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    firstAudioMessageId
                )
                this@runTest.advanceUntilIdle()

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)

                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second audio message
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    secondAudioMessageId
                )
                this@runTest.advanceUntilIdle()

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Completed)
                }
                awaitItem() // seekToAudioPosition
                awaitAndAssertStateUpdate { state ->
                    assertEquals(2, state.size)

                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing first audio message again, resuming it
                conversationAudioMessagePlayer.playAudio(
                    ConversationId("some-dummy-value", "some.dummy.domain"),
                    firstAudioMessageId
                )
                this@runTest.advanceUntilIdle()
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[secondAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Stopped)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Completed)
                }
                awaitItem() // seekToAudioPosition
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[firstAudioMessageIdWrapper]
                    assert(currentState != null)

                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }
                cancelAndIgnoreRemainingEvents()
            }

            with(arrangement) {
                verify(exactly = 3) { mediaPlayer.prepare() }
                verify(exactly = 3) { mediaPlayer.setDataSource(any(), any()) }
                verify(exactly = 3) { mediaPlayer.start() }

                verify(exactly = 1) { mediaPlayer.seekTo(any()) }
            }
        }

    @Test
    fun givenTheSuccessFullAssetFetch_whenPlayingDifferentAudioAfterFirstOneIsPlayedAndSecondStoppedAndResume_thenEmitStatesAsExpected() =
        runTest {
            val (arrangement, conversationAudioMessagePlayer) = Arrangement()
                .withSuccessFullAssetFetch()
                .withCurrentSession()
                .withAudioMediaPlayerReturningTotalTime(1000)
                .withMediaPlayerPlaying()
                .arrange()

            val testAudioMessageId = "some-dummy-message-id"
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val messageIdWrapper = MessageIdWrapper(conversationId, testAudioMessageId)

            conversationAudioMessagePlayer.observableAudioMessagesState.test {
                // skip first emit from onStart
                awaitItem()
                // playing first time
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    testAudioMessageId
                )
                this@runTest.advanceUntilIdle()

                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Fetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.SuccessfulFetching)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assertEquals(currentState!!.wavesMask, Arrangement.WAVES_MASK)
                }
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }
                awaitItem() // currentPosition update
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    val totalTime = currentState!!.totalTimeInMs
                    assert(totalTime is AudioState.TotalTimeInMs.Known)
                    assert((totalTime as AudioState.TotalTimeInMs.Known).value == 1000)
                }

                // playing second time
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    testAudioMessageId
                )
                this@runTest.advanceUntilIdle()
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Paused)
                }

                // stop the media player by mocking the return value of isPlaying
                arrangement.withMediaPlayerStopped()

                // playing third time
                conversationAudioMessagePlayer.playAudio(
                    conversationId,
                    testAudioMessageId
                )
                this@runTest.advanceUntilIdle()
                awaitAndAssertStateUpdate { state ->
                    val currentState = state[messageIdWrapper]
                    assert(currentState != null)
                    assert(currentState!!.audioMediaPlayingState is AudioMediaPlayingState.Playing)
                }

                cancelAndIgnoreRemainingEvents()
            }

            with(arrangement) {
                verify(exactly = 1) { mediaPlayer.prepare() }
                verify(exactly = 1) { mediaPlayer.setDataSource(any(), any()) }
                verify(exactly = 2) { mediaPlayer.start() }
                verify(exactly = 1) { mediaPlayer.pause() }

                verify(exactly = 0) { mediaPlayer.seekTo(any()) }
            }
        }

    @Test
    fun givenTheSuccessFullAssetFetch_whenAudioSpeedChanged_thenMediaPlayerParamsWereUpdated() = runTest {
        val params = PlaybackParams()
        val (arrangement, conversationAudioMessagePlayer) = Arrangement()
            .withSuccessFullAssetFetch()
            .withCurrentSession()
            .withAudioMediaPlayerReturningParams(params)
            .arrange()

        // when
        conversationAudioMessagePlayer.setSpeed(AudioSpeed.MAX)

        // then
        verify(exactly = 1) { arrangement.mediaPlayer.playbackParams = params.setSpeed(2F) }
    }

    @Test
    fun givenPlayingAudioMessage_whenStopAudioCalled_thenServiceStoppedAndAudioFocusAbandoned() = runTest {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement()
            .withAudioMediaPlayerReturningTotalTime(1000)
            .withSuccessFullAssetFetch()
            .withCurrentSession()
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            // skip first emit from onStart
            awaitItem()
            conversationAudioMessagePlayer.playAudio(
                conversationId,
                testAudioMessageId
            )
            this@runTest.advanceUntilIdle()

            conversationAudioMessagePlayer.forceToStopCurrentAudioMessage()

            cancelAndIgnoreRemainingEvents()
        }

        with(arrangement) {
            verify(exactly = 1) { audioFocusHelper.abandon() }
            verify(exactly = 1) { servicesManager.stopPlayingAudioMessageService() }
        }
    }

    private suspend fun <T> TurbineTestContext<T>.awaitAndAssertStateUpdate(assertion: (T) -> Unit) {
        val state = awaitItem()
        assert(state != null)

        assertion(state)
    }
}

class Arrangement {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var coreLogic: CoreLogic

    @MockK
    lateinit var mediaPlayer: MediaPlayer

    @MockK
    lateinit var wavesMaskHelper: AudioWavesMaskHelper

    @MockK
    lateinit var servicesManager: ServicesManager

    @MockK
    lateinit var audioFocusHelper: AudioFocusHelper

    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    private val conversationAudioMessagePlayer by lazy {
        ConversationAudioMessagePlayer(
            context,
            mediaPlayer,
            wavesMaskHelper,
            { servicesManager },
            audioFocusHelper,
            coreLogic,
            testScope
        )
    }

    init {
        MockKAnnotations.init(this, relaxed = true)

        every { wavesMaskHelper.getWaveMask(any<Path>()) } returns WAVES_MASK
        every { wavesMaskHelper.clear() } returns Unit
        every { mediaPlayer.currentPosition } returns 100

        every { servicesManager.stopPlayingAudioMessageService() } returns Unit
        every { servicesManager.startPlayingAudioMessageService() } returns Unit

        every { audioFocusHelper.setListener(any(), any()) } returns Unit
        every { audioFocusHelper.abandon() } returns Unit
        every { audioFocusHelper.request() } returns true
    }

    fun withCurrentSession() = apply {
        coEvery { coreLogic.getGlobalScope().session.currentSession.invoke() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(UserId("some-user-value", "some.user.domain"))
        )
    }

    fun withSuccessFullAssetFetch() = apply {
        coEvery {
            coreLogic.getSessionScope(any()).messages.getAssetMessage.invoke(any<ConversationId>(), any<String>())
        } returns CompletableDeferred(
            MessageAssetResult.Success(
                decodedAssetPath = FakeKaliumFileSystem().selfUserAvatarPath(),
                assetSize = 0,
                assetName = "some-dummy-asset-name"
            )
        )
    }

    fun withMediaPlayerPlaying() = apply {
        every { mediaPlayer.isPlaying } returns true
    }

    fun withMediaPlayerStopped() = apply {
        every { mediaPlayer.isPlaying } returns false
    }

    fun withAudioMediaPlayerReturningTotalTime(total: Int) = apply {
        every { mediaPlayer.duration } returns total
    }

    fun withAudioMediaPlayerReturningParams(params: PlaybackParams = PlaybackParams()) = apply {
        every { mediaPlayer.playbackParams } returns params
    }

    fun arrange() = this to conversationAudioMessagePlayer

    companion object {
        val WAVES_MASK = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    }
}
