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
package com.wire.android.ui.home

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.feature.e2ei.SyncCertificateRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.ObserveCertificateRevocationForSelfClientUseCase
import com.wire.kalium.logic.feature.featureConfig.FeatureFlagsSyncWorker
import com.wire.kalium.logic.feature.server.UpdateApiVersionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AppSyncViewModelTest {
    @Test
    fun `when startSyncingAppConfig is called then it should call the use case`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withObserveCertificateRevocationForSelfClient()
            withFeatureFlagsSyncWorker()
            withSyncCertificateRevocationListUseCase()
            withUpdateApiVersions()
        }

        viewModel.startSyncingAppConfig()
        advanceUntilIdle()

        coVerify { arrangement.observeCertificateRevocationForSelfClient.invoke() }
        coVerify { arrangement.syncCertificateRevocationListUseCase.invoke() }
        coVerify { arrangement.featureFlagsSyncWorker.execute() }
        coVerify { arrangement.updateApiVersions() }
    }

    @Test
    fun `when startSyncingAppConfig is called multiple times then it should call the use case with delay`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withObserveCertificateRevocationForSelfClient(1000)
            withFeatureFlagsSyncWorker(1000)
            withSyncCertificateRevocationListUseCase(1000)
            withUpdateApiVersions(1000)
        }

        viewModel.startSyncingAppConfig()
        viewModel.startSyncingAppConfig()
        viewModel.startSyncingAppConfig()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.observeCertificateRevocationForSelfClient.invoke() }
        coVerify(exactly = 1) { arrangement.syncCertificateRevocationListUseCase.invoke() }
        coVerify(exactly = 1) { arrangement.featureFlagsSyncWorker.execute() }
        coVerify(exactly = 1) { arrangement.updateApiVersions() }
    }

    private class Arrangement {

        @MockK
        lateinit var syncCertificateRevocationListUseCase: SyncCertificateRevocationListUseCase

        @MockK
        lateinit var observeCertificateRevocationForSelfClient: ObserveCertificateRevocationForSelfClientUseCase

        @MockK
        lateinit var featureFlagsSyncWorker: FeatureFlagsSyncWorker

        @MockK
        lateinit var updateApiVersions: UpdateApiVersionsUseCase

        init {
            MockKAnnotations.init(this)
        }

        private val viewModel = AppSyncViewModel(
            syncCertificateRevocationListUseCase,
            observeCertificateRevocationForSelfClient,
            featureFlagsSyncWorker,
            updateApiVersions
        )

        @OptIn(InternalCoroutinesApi::class)
        fun withObserveCertificateRevocationForSelfClient(delayMs: Long = 0) {
            coEvery { observeCertificateRevocationForSelfClient.invoke() } coAnswers {
                delay(delayMs)
            }
        }

        fun withSyncCertificateRevocationListUseCase(delayMs: Long = 0) {
            coEvery { syncCertificateRevocationListUseCase.invoke() } coAnswers {
                delay(delayMs)
            }
        }

        fun withFeatureFlagsSyncWorker(delayMs: Long = 0) {
            coEvery { featureFlagsSyncWorker.execute() } coAnswers {
                delay(delayMs)
            }
        }

        fun withUpdateApiVersions(delayMs: Long = 0) {
            coEvery { updateApiVersions() } coAnswers {
                delay(delayMs)
            }
        }

        fun arrange(block: Arrangement.() -> Unit) = apply(block).let {
            this to viewModel
        }
    }
}
