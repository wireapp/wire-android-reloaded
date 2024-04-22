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
package com.wire.android.ui

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.ui.calling.CallActivityViewModel
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import kotlinx.coroutines.flow.flowOf

class CallActivityViewModelTest {

    @Test
    fun `given no current, when checking screenshot censoring config, then return false`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Failure.SessionNotFound)
                .arrange()

            val result = viewModel.isScreenshotCensoringConfigEnabled()

            assertEquals(false, result.await())
        }

    @Test
    fun `given screenshot censoring enabled, when checking screenshot censoring config, then return true`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Success(accountInfo))
                .withScreenshotCensoringConfigReturning(ObserveScreenshotCensoringConfigResult.Enabled.ChosenByUser)
                .arrange()

            val result = viewModel.isScreenshotCensoringConfigEnabled()

            assertEquals(true, result.await())
        }


    @Test
    fun `given screenshot censoring disabled, when checking screenshot censoring config, then return false`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withCurrentSessionReturning(CurrentSessionResult.Success(accountInfo))
                .withScreenshotCensoringConfigReturning(ObserveScreenshotCensoringConfigResult.Disabled)
                .arrange()

            val result = viewModel.isScreenshotCensoringConfigEnabled()

            assertEquals(false, result.await())
        }


    private class Arrangement {

        @MockK
        private lateinit var observeScreenshotCensoringConfigUseCaseProviderFactory: ObserveScreenshotCensoringConfigUseCaseProvider.Factory

        @MockK
        private lateinit var currentSession: CurrentSessionUseCase

        @MockK
        private lateinit var observeScreenshotCensoringConfig: ObserveScreenshotCensoringConfigUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            every { observeScreenshotCensoringConfigUseCaseProviderFactory.create(any()).observeScreenshotCensoringConfig } returns
                    observeScreenshotCensoringConfig
        }

        private val viewModel by lazy {
            CallActivityViewModel(
                dispatchers = TestDispatcherProvider(),
                currentSession = currentSession,
                observeScreenshotCensoringConfigUseCaseProviderFactory = observeScreenshotCensoringConfigUseCaseProviderFactory,
            )
        }

        fun arrange() = this to viewModel

        suspend fun withCurrentSessionReturning(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        suspend fun withScreenshotCensoringConfigReturning(result: ObserveScreenshotCensoringConfigResult) = apply {
            coEvery { observeScreenshotCensoringConfig() } returns flowOf(result)
        }
    }

    companion object {
        val accountInfo  = AccountInfo.Valid(userId = UserId("userId", "domain"))
    }
}