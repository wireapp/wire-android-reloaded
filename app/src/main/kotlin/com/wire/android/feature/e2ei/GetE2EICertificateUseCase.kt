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
package com.wire.android.feature.e2ei

import android.content.Context
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.getActivity
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.e2ei.usecase.EnrollE2EIUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.qualifiers.ApplicationContext
import com.wire.kalium.logic.functional.left
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO remove it
class GetE2EICertificateUseCase @Inject constructor(
    private val enrollE2EI: EnrollE2EIUseCase,
    @ApplicationContext private val applicationContext: Context,
    val dispatcherProvider: DispatcherProvider
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private lateinit var initialEnrollmentResult: E2EIEnrollmentResult.Initialized
    lateinit var enrollmentResultHandler: (Either<E2EIFailure, E2EIEnrollmentResult>) -> Unit

    operator fun invoke(
        isNewClient: Boolean,
        enrollmentResultHandler: (Either<E2EIFailure, E2EIEnrollmentResult>) -> Unit
    ) {
        this.enrollmentResultHandler = enrollmentResultHandler
        scope.launch {
            enrollE2EI.initialEnrollment(isNewClientRegistration = isNewClient).fold({
                enrollmentResultHandler(Either.Left(it))
            }, {
                initialEnrollmentResult = it
                OAuthUseCase(applicationContext, it.target, it.oAuthClaims, it.oAuthState).launch(
                    applicationContext.getActivity()!!.activityResultRegistry,
                    ::oAuthResultHandler
                )
            })
        }
    }

    private fun oAuthResultHandler(oAuthResult: OAuthUseCase.OAuthResult) {
        scope.launch {
            when (oAuthResult) {
                is OAuthUseCase.OAuthResult.Success -> {
                    enrollmentResultHandler(
                        enrollE2EI.finalizeEnrollment(
                            oAuthResult.idToken,
                            oAuthResult.authState,
                            initialEnrollmentResult
                        )
                    )
                }

                is OAuthUseCase.OAuthResult.Failed -> {
                    enrollmentResultHandler(E2EIFailure.OAuth(oAuthResult.reason).left())
                }
            }
        }
    }
}
